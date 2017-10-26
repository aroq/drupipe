package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction
import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

class Docman extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def init() {
        jsonConfig()
    }

    // For compatibility with previous versions.
    def jsonConfig() {
        info()

        def docrootConfigJson = script.readFile("${context.projectConfigPath}/${action.params.docmanJsonConfigFile}")
        if (context.env.gitlabSourceNamespace) {
            context.projectName = utils.projectNameByGroupAndRepoName(script, docrootConfigJson, context.env.gitlabSourceNamespace, context.env.gitlabSourceRepoName)
        }
        script.echo "PROJECT NAME: ${context.projectName}"

        context
    }

    def info() {
        script.drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", action.params)
        script.echo "Config repo: ${context.configRepo}"
        prepare()
        script.drupipeShell(
            """
        cd ${context.docrootDir}
        docman info full config.json
        """, action,params
        )
    }

    def build() {
        init()
        deploy()
        context
    }

    def stripedBuild() {
        info()
        def docrootConfigJson = script.readFile("${context.projectConfigPath}/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson, 'nexus')
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd docroot
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag(context)}
            """, action.params
        )
        if (!context['builder']) {
            context['builder'] = [:]
        }
        context.builder['buildDir'] = "${context.docrootDir}/master"
        context.builder['buildName'] = context.jenkinsFolderName
        context.builder['version'] = (new Date()).format('yyyy-MM-dd--hh-mm-ss')
        context
    }

    def releaseBuild() {
        info()
        def docrootConfigJson = script.readFile("${context.projectConfigPath}/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson)
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd docroot
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag(context)}
            """, action.params
        )
        context
    }

    def deploy() {
        script.drupipeShell(
            """
            cd docroot
            docman deploy git_target ${context.projectName} branch ${context.version} ${forceFlag(context)}
            """, action.params
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
        """, action.params
        )
        if (context.configRepo && !script.fileExists(context.docrootDir)) {
            script.drupipeShell(
                """
            if [ "${context.force}" == "1" ]; then
              rm -fR ${context.docrootDir}
            fi
            docman init ${context.docrootDir} ${context.configRepo} -s
            """, action.params
            )
            context.dir
        }
    }

    @NonCPS
    def component_versions(docrootConfigJson, mode = 'default') {
        def docmanConfig = JsonSlurperClassic.newInstance().parseText(docrootConfigJson)
        def result = ''
        if (mode == 'nexus') {
            def versions = []
            docmanConfig.projects.each { project ->
                if (context["${project.key}_version"]) {
                    versions << /"${project.key}": / + context["${project.key}_version"]
                }
            }
            if (versions) {
                result = /--config="{\"projects\": {${versions.join(', ').replaceAll("\"", "\\\\\"")}}}"/
            }
        }
        else if (mode == 'default') {
            def projects = [:]
            docmanConfig.projects.each { project ->
                if (context["${project.key}_version"]) {
                    projects[project.key] = [states: [stable: [type: 'branch', version: context["${project.key}_version"]]]]
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
        script.drupipeShell(
            """docman bump stable -n""", action.params
        )
    }

    def getStable() {
        def info = script.readYaml(file: "info.yaml")
        script.echo "STABLE VERSION: ${info.version}"

        String repo = script.scm.getUserRemoteConfigs()[0].getUrl()

        def sourceObject = [
            name: 'stable_version',
            type: 'git',
            path: context.jenkinsParams.workingDir,
            url: repo,
            branch: info.version,
            mode: 'shell',
        ]

        this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], context)
    }
}

