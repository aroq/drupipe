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
        if (action.params.deployHandler) {
            context << script.drupipeAction([action: "${action.params.deployHandler}.deploy", params: context.builder.artifactParams], context)
        }
        else {
            script.echo "No deploy handler defined"
        }
    }

    def operations() {
        setParams()
        if (action.params.operationsHandler) {
            context << script.drupipeAction([action: "${action.params.operationsHandler}.operations"], context)
        }
        else {
            script.echo "No deploy handler defined"
        }
    }

    def retrieveArtifact() {
        if (!context['builder']) {
            context['builder'] = [:]
        }
        script.drupipeAction([action: "${action.params.buildHandler}.artifactParams"], context)
        context << script.drupipeAction([action: "${action.params.artifactHandler}.retrieve", params: context.builder.artifactParams], context)
        context.projectName = 'master'
        context << [returnConfig: true]
    }

}
