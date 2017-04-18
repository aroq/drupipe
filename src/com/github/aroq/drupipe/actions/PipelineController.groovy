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
        // Dispatch the action.
        context << script.drupipeAction([action: "${action.params.buildHandler.handler}.${action.params.buildHandler.method}"], context)
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
        if (action.params.operationsHandler && action.params.operationsHandler.handler) {
            context << script.drupipeAction([action: "${action.params.operationsHandler.handler}.${action.params.operationsHandler.method}"], context)
        }
        else {
            script.echo "No operations handler defined"
        }
    }

    def retrieveArtifact() {
        if (!context['builder']) {
            context['builder'] = [:]
        }
        if (action.params.buildHandler && action.params.buildHandler.handler) {
            script.drupipeAction([action: "${action.params.buildHandler.handler}.artifactParams"], context)
            context << script.drupipeAction([action: "${action.params.artifactHandler.handler}.${action.params.artifactHandler.method}", params: context.builder.artifactParams], context)
            context.projectName = 'master'
        }
        else {
            script.echo "No build handler defined"
        }
        context
    }

}
