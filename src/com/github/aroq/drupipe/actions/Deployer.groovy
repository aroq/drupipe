package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Deployer extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def setParams() {
    }

    def deploy() {
        setParams()
        retrieveArtifact()
        if (action.params.deployHandler.handler) {
            context << script.drupipeAction([action: "${action.params.deployHandler.handler}.${action.params.deployHandler.method}", params: context.builder.artifactParams], context)
        }
        else {
            script.echo "No deploy handler defined"
        }
    }

    def operations() {
        setParams()
        if (action.params.operationsHandler.handler) {
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
        script.drupipeAction([action: "${action.params.buildHandler.handler}.artifactParams"], context)
        context << script.drupipeAction([action: "${action.params.artifactHandler.handler}.${action.params.artifactHandler.method}", params: context.builder.artifactParams], context)
        context.projectName = 'master'
        context << [returnConfig: true]
    }

}
