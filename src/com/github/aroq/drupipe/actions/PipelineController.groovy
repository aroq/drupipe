package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class PipelineController extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def build() {
        if (!context['builder']) {
            context['builder'] = [:]
        }
        if (action.params.buildHandler && action.params.buildHandler.handler) {
            // Dispatch the action.
            context << script.drupipeAction([action: "${action.params.buildHandler.handler}.${action.params.buildHandler.method}"], context)
        }
        else {
            script.echo "No builder handler defined"
        }
    }

    // TODO: move to artifact handler.
    def createArtifact() {
        def sourceDir = context.builder['buildDir']
        def fileName = "${context.builder['buildName']}-${context.builder['version']}.tar.gz"
        context.builder['artifactFileName'] = fileName
        context.builder['groupId'] = context.jenkinsFolderName

        script.drupipeShell(
            """
                rm -fR ${sourceDir}/.git
                tar -czf ${fileName} ${sourceDir}
            """, context << [shellCommandWithBashLogin: true]
        )
        context
    }

    def deploy() {
        if (action.params.deployHandler && action.params.deployHandler.handler) {
            retrieveArtifact()
            context << script.drupipeAction([action: "${action.params.deployHandler.handler}.${action.params.deployHandler.method}", params: context.builder.artifactParams], context)
        }
        else {
            script.echo "No deploy handler defined"
        }
    }

    def operations() {
        if (context.operationsMode == 'no-ops') {
            script.echo "No operations mode (no-ops) is selected"
        }
        else {
            if (action.params.operationsHandler && action.params.operationsHandler.handler) {
                context << script.drupipeAction([action: "${action.params.operationsHandler.handler}.${action.params.operationsHandler.method}"], context)
            }
            else {
                script.echo "No operations handler defined"
            }
        }
    }

    def test() {
        if (action.params.testHandler && action.params.testHandler.handler) {
            context << script.drupipeAction([action: "${action.params.testHandler.handler}.${action.params.testHandler.method}"], context)
        }
        else {
            script.echo "No test handler defined"
        }
    }

    def retrieveArtifact() {
        if (!context['builder']) {
            context['builder'] = [:]
        }
        if (action.params.artifactHandler && action.params.artifactHandler.handler) {
            //script.drupipeAction([action: "${action.params.buildHandler.handler}.artifactParams"], context)
            artifactParams()
            context << script.drupipeAction([action: "${action.params.artifactHandler.handler}.${action.params.artifactHandler.method}", params: context.builder.artifactParams], context)
            if (!context.projectName) {
                context.projectName = 'master'
            }
        }
        else {
            script.echo "No artifact handler defined"
        }
        context
    }

    def repoParams(String configPath) {
        //info()
        def repo
        def masterInfoFile = "${context.projectConfigPath}/${configPath}/info.yaml"
        drupipeShell("""
            ls -al ${context.projectConfigPath}
            ls -al ${context.projectConfigPath}/${configPath}
            """, context
        )
        if (script.fileExists(masterInfoFile)) {
            script.echo "File exists: ${masterInfoFile}"
            def masterConfig = script.readYaml(file: masterInfoFile)
            script.echo "MASTER CONFIG: ${masterConfig}"
            repo = masterConfig.type == 'root' ? masterConfig.repo : masterConfig.root_repo
        }
        else {
            script.echo "File NOT exists: ${masterInfoFile}"
            repo = context.components.master.root_repo ? context.components.master.root_repo : context.components.master.repo
        }
        script.echo "REPO: ${repo}"

        String reference
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


}
