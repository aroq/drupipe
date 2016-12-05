package com.github.aroq.workflowlibs.actions

import groovy.json.JsonSlurper

def jsonConfig(params) {
    info(params)
    docrootConfigJson = readFile("${params.docmanConfigPath}/${params.docmanJsonConfigFile}")

    def pn = ''

    if (env.gitlabSourceNamespace) {
       pn = projectNameByGroupAndRepoName(this, docrootConfigJson, env.gitlabSourceNamespace, env.gitlabSourceRepoName)
    }

    if (pn) {
        params.projectName = pn
    }
    else if (projectName) {
        params.projectName = projectName
    }
    else {
        // TODO: refactor it
        params.projectName = 'common'
    }
    echo "PROJECT NAME: ${params.projectName}"

    params << [returnConfig: true]
}

def info(params) {
    if (params.force == '1') {
        echo "Force mode"
        sh(
            """#!/bin/bash -l
            if [ "${params.force}" == "1" ]; then
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
    if (params.force == 1) {
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
        if [ "${params.force}" == "1" ]; then
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
def projectNameByGroupAndRepoName(script, docrootConfigJson, groupName, repoName) {
    // TODO: Refactor it.
    groupName = groupName.toLowerCase()
    repoName = repoName.toLowerCase()
    docmanConfig = JsonSlurper.newInstance().parseText(docrootConfigJson)
    result = ''
    docmanConfig.projects.each { project ->
        def repo = project.value['repo'];
        if (repo) {
            script.echo "REPO: ${repo.toLowerCase()}"
            script.echo "GITLAB: ${groupName}/${repoName}"
            if (repo) {
                if (repo.toLowerCase().contains("${groupName}/${repoName}")) {
                    result = project.value['name']
                }
            }
        }
    }
    result
}

