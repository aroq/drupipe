package com.github.aroq.drupipe.actions

import groovy.json.JsonSlurperClassic

def init(params) {
    jsonConfig(params)
}

// For compatibility with previous versions.
def jsonConfig(params) {
    info(params)

    utils = new com.github.aroq.drupipe.Utils()
    docrootConfigJson = readFile("${params.docmanConfigPath}/${params.docmanJsonConfigFile}")
    if (env.gitlabSourceNamespace) {
       params.projectName = utils.projectNameByGroupAndRepoName(this, docrootConfigJson, env.gitlabSourceNamespace, env.gitlabSourceRepoName)
    }
    echo "PROJECT NAME: ${params.projectName}"

    params << [returnConfig: true]
}

def info(params) {
    echo "Config repo: ${params.configRepo}"
    prepare(params)
    drupipeShell(
        """
        cd ${params.docrootDir}
        docman info full config.json
        """, params << [shellCommandWithBashLogin: true]
    )
}

def build(params) {
    init(params)
    deploy(params)
    params << [returnConfig: true]
    params
}

def stripedBuild(params) {
    info(params)
    docrootConfigJson = readFile("${params.docmanConfigPath}/${params.docmanJsonConfigFile}")
    def componentVersions = component_versions(params, docrootConfigJson)
    echo "Component versions:${componentVersions}"

    drupipeShell(
        """
        cd docroot
        docman build striped stable ${componentVersions} ${forceFlag(params)}
        """, params << [shellCommandWithBashLogin: true]
    )
    if (!params['builder']) {
        params['builder'] = [:]
    }
    params.builder['buildDir'] = "${params.docrootDir}/master"
    params.builder['buildName'] = params.jenkinsFolderName
    params.builder['version'] = (new Date()).format('yyyy-MM-dd--hh-mm-ss')
    params << [returnConfig: true]
    params
}

def deploy(params) {
    drupipeShell(
        """
        cd docroot
        docman deploy git_target ${params.projectName} branch ${version} ${forceFlag(params)}
        """, params << [shellCommandWithBashLogin: true]
    )
}

def forceFlag(params) {
    def flag = ''
    if (params.force == '1') {
        flag = '-f'
    }
    flag
}

def prepare(params) {
    echo "FORCE MODE: ${params.force}"
    drupipeShell(
        """
        if [ "${params.force}" == "1" ]; then
          rm -fR ${params.docrootDir}
        fi
        """, params << [shellCommandWithBashLogin: true]
    )
    if (params.configRepo && !fileExists(params.docrootDir)) {
        drupipeShell(
            """
            if [ "${params.force}" == "1" ]; then
              rm -fR ${params.docrootDir}
            fi
            docman init ${params.docrootDir} ${params.configRepo} -s
            """, params << [shellCommandWithBashLogin: true]
        )
        params.dir
    }
}

@NonCPS
def component_versions(params, docrootConfigJson) {
    docmanConfig = JsonSlurperClassic.newInstance().parseText(docrootConfigJson)
    versions = []
    docmanConfig.projects.each { project ->
        if (params["${project.key}_version"]) {
            versions << /"${project.key}": / + params["${project.key}_version"]
        }
    }
    if (versions) {
        result = /--config="{\"projects\": {${versions.join(', ').replaceAll("\"", "\\\\\"")}}}"/
    }
    result
}

