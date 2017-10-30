package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper
import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

class Docman extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    // TODO: Refactor all.

    def init() {
        jsonConfig()
    }

    // For compatibility with previous versions.
    def jsonConfig() {
        info()

        def docrootConfigJson = script.readFile("${action.pipeline.context.projectConfigPath}/${action.params.docmanJsonConfigFile}")
        if (action.pipeline.context.env.gitlabSourceNamespace) {
            action.pipeline.context.projectName = utils.projectNameByGroupAndRepoName(script, docrootConfigJson, action.pipeline.context.env.gitlabSourceNamespace, action.pipeline.context.env.gitlabSourceRepoName)
        }
        script.echo "PROJECT NAME: ${action.pipeline.context.jenkinsParams.projectName}"
        [:]
    }

    def info() {
        script.drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", action.params)
        script.echo "Config repo: ${action.pipeline.context.configRepo}"
        prepare()
        script.drupipeShell(
            """
        cd ${action.pipeline.context.docrootDir}
        docman info full config.json
        """, action.params
        )
        [:]
    }

    def build() {
        init()
        utils.debugLog(action.pipeline.context, action.pipeline.context, "CONFIG CONTEXT - ${action.fullName} - 1", [debugMode: 'json'], [], true)
        deploy()
        utils.debugLog(action.pipeline.context, action.pipeline.context, "CONFIG CONTEXT - ${action.fullName} - 2", [debugMode: 'json'], [], true)
        [:]
    }

    def stripedBuild() {
        info()
        def docrootConfigJson = script.readFile("${action.pipeline.context.projectConfigPath}/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson, 'nexus')
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd docroot
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag(action.pipeline.context)}
            """, action.params
        )
        if (!action.pipeline.context['builder']) {
            action.pipeline.context['builder'] = [:]
        }
        action.pipeline.context.builder['buildDir'] = "${action.pipeline.context.docrootDir}/master"
        action.pipeline.context.builder['buildName'] = action.pipeline.context.jenkinsFolderName
        action.pipeline.context.builder['version'] = (new Date()).format('yyyy-MM-dd--hh-mm-ss')
        [:]
    }

    def releaseBuild() {
        info()
        def docrootConfigJson = script.readFile("${action.pipeline.context.projectConfigPath}/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson)
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd docroot
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag()}
            """, action.params
        )
        action.pipeline.context
    }

    def deploy() {
        script.drupipeShell(
            """
            cd docroot
            docman deploy git_target ${action.pipeline.context.jenkinsParams.projectName} branch ${action.pipeline.context.jenkinsParams.version} ${forceFlag()}
            """, action.params
        )
    }

    def forceFlag() {
        def flag = ''
        if (action.pipeline.context.jenkinsParams.force == '1') {
            flag = '-f'
        }
        flag
    }

    def prepare() {
        script.echo "FORCE MODE: ${action.pipeline.context.jenkinsParams.force}"
        script.drupipeShell(
            """
        if [ "${action.pipeline.context.force}" == "1" ]; then
          rm -fR ${action.pipeline.context.docrootDir}
        fi
        """, action.params
        )
        if (action.pipeline.context.configRepo && !script.fileExists(action.pipeline.context.docrootDir)) {
            script.drupipeShell(
                """
            if [ "${action.pipeline.context.force}" == "1" ]; then
              rm -fR ${action.pipeline.context.docrootDir}
            fi
            docman init ${action.pipeline.context.docrootDir} ${action.pipeline.context.configRepo} -s
            """, action.params
            )
            action.pipeline.context.dir
        }
    }

    @NonCPS
    def component_versions(docrootConfigJson, mode = 'default') {
        def docmanConfig = JsonSlurperClassic.newInstance().parseText(docrootConfigJson)
        def result = ''
        if (mode == 'nexus') {
            def versions = []
            docmanConfig.projects.each { project ->
                if (action.pipeline.context["${project.key}_version"]) {
                    versions << /"${project.key}": / + action.pipeline.context["${project.key}_version"]
                }
            }
            if (versions) {
                result = /--config="{\"projects\": {${versions.join(', ').replaceAll("\"", "\\\\\"")}}}"/
            }
        }
        else if (mode == 'default') {
            def projects = [:]
            docmanConfig.projects.each { project ->
                if (action.pipeline.context["${project.key}_version"]) {
                    projects[project.key] = [states: [stable: [type: 'branch', version: action.pipeline.context["${project.key}_version"]]]]
                }
            }
            if (projects) {
                def json = JsonOutput.toJson([projects: projects]).replaceAll("\"", "\\\\\"")
                result = /--config="${json}"/
            }
        }
        result
    }

    def bumpStable() {
        script.drupipeShell("docman bump stable -n", action.params)
    }

    def getStable() {
        def info = script.readYaml(file: "info.yaml")
        script.echo "STABLE VERSION: ${info.version}"

        String repo = script.scm.getUserRemoteConfigs()[0].getUrl()

        def sourceObject = [
            name: 'stable_version',
            type: 'git',
            path: action.pipeline.context.jenkinsParams.workingDir,
            url: repo,
            branch: info.version,
            mode: 'shell',
        ]

        this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], action.pipeline.context)
    }
}

