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
    }

    def operations() {
        setParams()
    }

    def retrieveArtifact() {
        if (!context['builder']) {
            context['builder'] = [:]
        }
        script.drupipeAction([action: "${action.params.buildHandler}.artifactParams"], context)
        context << script.drupipeAction([action: "${action.params.deployHandler}.retrieve", params: context.builder.artifactParams], context)
        context.projectName = 'master'
        script.drupipeShell(
            """
                cat docroot/master/version.properties
            """, context << [shellCommandWithBashLogin: true]
        )
        context << [returnConfig: true]
    }

}
