package com.github.aroq.drupipe.actions

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

class Docman extends BaseAction {

    // TODO: Refactor all.

    def init() {
        jsonConfig()
    }

    // For compatibility with previous versions.
    def jsonConfig() {
        info()

        def docrootConfigJson = script.readFile("${action.pipeline.context.docmanDir}/config/${action.params.docmanJsonConfigFile}")
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
        cd ${action.pipeline.context.docmanDir}
        docman info full config.json ${debugFlag()}
        """, action.params
        )
        [:]
    }

    def build() {
        init()
        deploy()
        [:]
    }

    def stripedBuild() {
        info()
        def docrootConfigJson = script.readFile("${action.pipeline.context.docmanDir}/config/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson, 'nexus')
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd ${action.pipeline.context.docmanDir}
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag()} ${debugFlag()}
            """, action.params
        )
        if (!action.pipeline.context['builder']) {
            action.pipeline.context['builder'] = [:]
        }
        if (script.fileExists("${action.pipeline.context.docmanDir}/master")) {
            action.pipeline.context.builder['buildDir'] = "${action.pipeline.context.docmanDir}/master"
        }
        else if (script.fileExists("${action.pipeline.context.docmanDir}/.docman/master")) {
            action.pipeline.context.builder['buildDir'] = "${action.pipeline.context.docmanDir}/.docman/master"
        }
        else {
            action.pipeline.context.builder['buildDir'] = "${action.pipeline.context.docrootDir}/master"
        }
        action.pipeline.context.builder['buildName'] = action.pipeline.context.jenkinsFolderName
        action.pipeline.context.builder['version'] = (new Date()).format('yyyy-MM-dd--hh-mm-ss')
        [:]
    }

    def releaseBuild() {
        info()
        def docrootConfigJson = script.readFile("${action.pipeline.context.docmanDir}/config/${action.params.docmanJsonConfigFile}")
        def componentVersions = component_versions(docrootConfigJson)
        script.echo "Component versions:${componentVersions}"

        script.drupipeShell(
            """
            cd ${action.pipeline.context.docmanDir}
            docman build ${action.params.build_type} ${action.params.state} ${componentVersions} ${forceFlag()} ${debugFlag()}
            """, action.params
        )
        action.pipeline.context
    }

    def deploy() {
        script.drupipeShell(
            """
            cd ${action.pipeline.context.docmanDir}
            docman deploy git_target ${action.pipeline.context.jenkinsParams.projectName} branch ${action.pipeline.context.jenkinsParams.version} ${forceFlag()} ${debugFlag()}
            """, action.params
        )
    }

    def debugFlag() {
        def flag = ''
        if (action.pipeline.context.jenkinsParams.debugEnabled == '1') {
            flag = '-d'
        }
        flag
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

        def configBranch = 'master'
        if (action.pipeline.context.tags.contains('single')) {
            if (action.pipeline.context.environmentParams && action.pipeline.context.environmentParams.containsKey('git_reference')) {
                configBranch = action.pipeline.context.environmentParams.git_reference
            }
            else if (action.pipeline.context.job && action.pipeline.context.job.containsKey('branch')) {
                configBranch = action.pipeline.context.job.branch
            }
        }

        script.drupipeShell(
        """
        rm -fR ${action.pipeline.context.docmanDir}
        docman init ${action.pipeline.context.docmanDir} ${action.pipeline.context.configRepo} -s --branch=${configBranch} ${debugFlag()}
        """, action.params
        )
        action.pipeline.context.dir
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
        script.drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", action.params)
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
