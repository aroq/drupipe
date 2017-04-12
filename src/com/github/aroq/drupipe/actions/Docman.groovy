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
        docman info full config.json ${mothershipConfigRepoOption()}
        """, context << [shellCommandWithBashLogin: true]
        )
    }

    def build() {
        init()
        deploy()
        context << [returnConfig: true]
        context
    }

    String mothershipConfigRepoOption() {
        " --config_repo=${script.env.MOTHERSHIP_REPO} --config_repo_branch=master"
    }

    def stripedBuild() {
        info()
        def docrootConfigJson = script.readFile("${context.docmanConfigPath}/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson, 'nexus')
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd docroot
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag(context)} ${mothershipConfigRepoOption()}
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

    def releaseBuild() {
        info()
        def docrootConfigJson = script.readFile("${context.docmanConfigPath}/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson)
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd docroot
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag(context)} ${mothershipConfigRepoOption()}
            """, context << [shellCommandWithBashLogin: true]
        )
        context
    }

    def deploy() {
        script.drupipeShell(
            """
            cd docroot
            docman deploy git_target ${context.projectName} branch ${context.version} ${forceFlag(context)} ${mothershipConfigRepoOption()}
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

    def repoParams(String configPath) {
        info()
        def repo = null
        def masterInfoFile = "docroot/config/${configPath}/info.yaml"
        if (script.fileExists(masterInfoFile)) {
            def masterConfig = script.readYaml(file: masterInfoFile)
            script.echo "MASTER CONFIG: ${masterConfig}"
            repo = masterConfig.type == 'root' ? masterConfig.repo : masterConfig.root_repo
        }
        else {
            repo = context.components.master.root_repo ? context.components.master.root_repo : context.components.master.repo
        }
        script.echo "REPO: ${repo}"

        String reference = null
        if (context.release) {
            reference = context.release
        }
        else {
            reference = context.environmentParams.git_reference
        }
        script.echo "reference: ${reference}"
        return [
            repoAddress: repo,
            reference: reference,
            // TODO: refactor it.
            projectName: configPath,
        ]
    }

    def artifactParams() {
        context.builder.artifactParams = repoParams('master')
        context
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
}

