package com.github.aroq.workflowlibs.actions

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

def info(params) {
    echo "Requesting docman for config..."
    if (force == 1) {
        sh(
            """#!/bin/bash -l
            if [ "${force}" == "1" ]; then
              FLAG="-f"
              rm -fR ${params.docrootDir}
            fi
            """
        )
    }

    try {
        if (config_repo) {
            configRepo = config_repo
        }
    }
    catch (err) {

    }
//    configRepo = jenkinsParam('config_repo')
//    if (params.configRepo) {
//        configRepo = params.configRepo
//    }
//    if (config_repo) {
//        configRepo = config_repo
//    }
    if (configRepo) {
        sh(
            """#!/bin/bash -l
            docman init ${params.docrootDir} ${configRepo} -s
            """
        )
    }
    sh(
        """#!/bin/bash -l
        cd ${params.docrootDir}
        docman info full config.json
        """
    )
    echo "Requesting docman for config... DONE."
}

def deploy(params) {
    echo "Docman deploy"
    def flag = ''
    if (force == 1) {
        flag = '-f'
    }
    sh(
        """#!/bin/bash -l
        if [ "${force}" == "1" ]; then
          rm -fR ${params.docrootDir}
        fi
        docman init ${params.docrootDir} ${config_repo} -s
        cd docroot
        docman deploy git_target ${projectName} branch ${version} ${flag}
        """
    )
}

def init(params) {
    echo "Docman init"
    def flag = ''
    if (force == 1) {
        flag = '-f'
    }
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
