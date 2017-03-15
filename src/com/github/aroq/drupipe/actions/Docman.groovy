package com.github.aroq.drupipe.actions

import groovy.json.JsonSlurperClassic

class Docman extends BaseAction {

    def init() {
        jsonConfig()
    }

    // For compatibility with previous versions.
    def jsonConfig() {
        info()

        def docrootConfigJson = script.readFile("${context.docmanConfigPath}/${action.params.docmanJsonConfigFile}")
        if (context.env.gitlabSourceNamespace) {
            context.projectName = utils.projectNameByGroupAndRepoName(script, docrootConfigJson, context.env.gitlabSourceNamespace, context.env.gitlabSourceRepoName)
        }
        script.echo "PROJECT NAME: ${context.projectName}"

        context << [returnConfig: true]
    }

    def info() {
        script.echo "Config repo: ${context.configRepo}"
        prepare()
        script.drupipeShell(
            """
        cd ${context.docrootDir}
        docman info full config.json
        """, context << [shellCommandWithBashLogin: true]
        )
    }

    def build() {
        init()
        deploy()
        context << [returnConfig: true]
        context
    }

    def stripedBuild() {
        info()
        def docrootConfigJson = script.readFile("${context.docmanConfigPath}/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson)
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
        cd docroot
        docman build striped stable ${componentVersions} ${forceFlag(context)}
        """, context << [shellCommandWithBashLogin: true]
        )
        if (!context['builder']) {
            context['builder'] = [:]
        }
        context.builder['buildDir'] = "${context.docrootDir}/master"
        context.builder['buildName'] = context.jenkinsFolderName
        context.builder['version'] = (new Date()).format('yyyy-MM-dd--hh-mm-ss')
        context << [returnConfig: true]
        context
    }

    def deploy() {
        script.drupipeShell(
            """
        cd docroot
        docman deploy git_target ${context.projectName} branch ${context.version} ${forceFlag(context)}
        """, context << [shellCommandWithBashLogin: true]
        )
    }

    def forceFlag(context) {
        def flag = ''
        if (context.force == '1') {
            flag = '-f'
        }
        flag
    }

    def prepare() {
        script.echo "FORCE MODE: ${context.force}"
        script.drupipeShell(
            """
        if [ "${context.force}" == "1" ]; then
          rm -fR ${context.docrootDir}
        fi
        """, context << [shellCommandWithBashLogin: true]
        )
        if (context.configRepo && !script.fileExists(context.docrootDir)) {
            script.drupipeShell(
                """
            if [ "${context.force}" == "1" ]; then
              rm -fR ${context.docrootDir}
            fi
            docman init ${context.docrootDir} ${context.configRepo} -s
            """, context << [shellCommandWithBashLogin: true]
            )
            context.dir
        }
    }

    @NonCPS
    def component_versions(docrootConfigJson) {
        def docmanConfig = JsonSlurperClassic.newInstance().parseText(docrootConfigJson)
        def versions = []
        docmanConfig.projects.each { project ->
            if (context["${project.key}_version"]) {
                versions << /"${project.key}": / + context["${project.key}_version"]
            }
        }
        def result = ''
        if (versions) {
            result = /--config="{\"projects\": {${versions.join(', ').replaceAll("\"", "\\\\\"")}}}"/
        }
        result
    }

}

