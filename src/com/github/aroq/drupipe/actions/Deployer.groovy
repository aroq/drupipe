package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Deployer extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def setParams() {
        def environment = context.environments[context.environment]
        def server = context.servers[environment['server']]
        def environmentParams = utils.merge(server.params, environment.params)
        utils.jsonDump(environmentParams, 'ENVIRONMENT PARAMS')
        action.params = utils.merge(action.params, environmentParams)
        utils.jsonDump(action.params, action.params)
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
        script.drupipeAction([action: "${action.params.builderHandler}.artifactParams"], context)
        context << script.drupipeAction([action: "${action.params.artifactHandler}.retrieve", params: context.builder.artifactParams], context)
        context.projectName = 'master'
        script.drupipeShell(
            """
                cat docroot/master/version.properties
            """, context << [shellCommandWithBashLogin: true]
        )
        context << [returnConfig: true]
    }

}
