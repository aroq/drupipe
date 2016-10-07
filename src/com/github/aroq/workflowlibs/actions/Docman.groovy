package com.github.aroq.workflowlibs.actions

import groovy.json.JsonSlurper

def config(params) {
    if (!params.docmanConfigType) {
        params.docmanConfigType = 'git'
    }
    if (params.docmanConfigType == 'git') {
        sourceObject = [
            name: 'docmanConfig',
            type: 'git',
            url: config_repo,
            path: params.docmanConfigPath,
            branch: 'master',
        ]
    }
    else {
        sourceObject = [
            name: 'docmanConfig',
            type: 'dir',
            path: params.docmanConfigPath,
        ]
    }
    actions = [
        [
            action: 'Docman.info',
        ],
        [
            action: 'Source.add',
            params: [source: sourceObject]
        ],
        [
            action: 'Source.loadConfig',
            params: [
                sourceName: 'docmanConfig',
                configType: 'groovy',
                configPath: params.docmanConfigFile
            ]
        ]
    ]

    params << executePipelineActionList(actions) {
        p = params
    }

    params << [returnConfig: true]
}

def jsonConfig(params) {
    docrootConfigJson = readFile("${params.docmanConfigPath}/${params.docmanJsonConfigFile}")

    projectName = projectNameByGroupAndRepoName(docrootConfigJson, env.gitlabSourceNamespace, env.gitlabSourceRepoName)
    if (projectName) {
        params.projectName = projectName
    }

    params << [returnConfig: true]
}

def info(params) {
    if (force == '1') {
        echo "Force mode"
        sh(
            """#!/bin/bash -l
            if [ "${force}" == "1" ]; then
              rm -fR ${params.docrootDir}
            fi
            """
        )
    }

    configRepo = false
    try {
        if (config_repo) {
            configRepo = config_repo
        }
    }
    catch (err) {

    }
    if (configRepo) {
        echo 'Docman init'
        sh('ls -l')
        sh(
            """#!/bin/bash -l
            docman init ${params.docrootDir} ${configRepo} -s
            """
        )
    }
    echo 'Docman info'
    sh(
        """#!/bin/bash -l
        cd ${params.docrootDir}
        docman info full config.json
        """
    )
}

def deploy(params) {
    def flag = ''
    if (force == 1) {
        flag = '-f'
    }

    if (params.projectName) {
        deployProjectName = params.projectName
    }
    else {
        deployProjectName = projectName
    }

    sh(
        """#!/bin/bash -l
        if [ "${force}" == "1" ]; then
          rm -fR ${params.docrootDir}
        fi
        docman init ${params.docrootDir} ${config_repo} -s
        cd docroot
        docman deploy git_target ${deployProjectName} branch ${version} ${flag}
        """
    )
}

def init(params) {
    if (params.configRepo) {
        configRepo = params.configRepo
    }
    if (config_repo) {
        configRepo = config_repo
    }
    if (configRepo) {
        sh(
            """#!/bin/bash -l
            docman init ${params.path} ${configRepo} -s
            """
        )
        params.dir
    }
    else {
        null
    }
}

@NonCPS
def projectNameByGroupAndRepoName(docrootConfigJson, groupName, repoName) {
    docmanConfig = JsonSlurper.newInstance().parseText(docrootConfigJson)
    result = ''
    docmanConfig.projects.each { project ->
        if (project.value['repo']?.contains("${groupName}/${repoName}")) {
            result = project.value['name']
        }
    }
    result
}

