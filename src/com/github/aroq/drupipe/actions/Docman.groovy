package com.github.aroq.drupipe.actions

def jsonConfig(params) {
    echo "jsonConfig params: ${params}"
    info(params)

    utils = new com.github.aroq.drupipe.Utils()
    docrootConfigJson = readFile("${params.docmanConfigPath}/${params.docmanJsonConfigFile}")
    if (env.gitlabSourceNamespace) {
       params.projectName = utils.projectNameByGroupAndRepoName(this, docrootConfigJson, env.gitlabSourceNamespace, env.gitlabSourceRepoName)
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
        drupipeShell(
            """#!/bin/bash -l
            if [ "${params.force}" == "1" ]; then
              rm -fR ${params.docrootDir}
            fi
            """, params
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

    echo "Config repo: ${configRepo}"

    if (configRepo && !fileExists('docroot')) {
        echo 'Docman init'
        drupipeShell(
            """#!/bin/bash -l
            docman init ${params.docrootDir} ${configRepo} -s
            """, params
        )
    }
    echo 'Docman info'
    drupipeShell(
        """#!/bin/bash -l
        cd ${params.docrootDir}
        docman info full config.json
        """, params
    )
}

def build(params) {
    jsonConfig(params)
    deploy(params)
    params << [returnConfig: true]
    params
}

def deploy(params) {
    echo "FORCE MODE: ${params.force}"
    def flag = ''
    if (params.force == '1') {
        flag = '-f'
    }

    if (params.projectName) {
        deployProjectName = params.projectName
    }
    else {
        deployProjectName = projectName
    }

    echo "docman deploy git_target ${deployProjectName} branch ${version} ${flag}"

    drupipeShell(
        """#!/bin/bash -l
        if [ "${params.force}" == "1" ]; then
          rm -fR ${params.docrootDir}
        fi
        docman init ${params.docrootDir} ${config_repo} -s
        cd docroot
        docman deploy git_target ${deployProjectName} branch ${version} ${flag}
        """, params
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
        drupipeShell(
            """#!/bin/bash -l
            docman init ${params.path} ${configRepo} -s
            """, params
        )
        params.dir
    }
    else {
        null
    }
}

