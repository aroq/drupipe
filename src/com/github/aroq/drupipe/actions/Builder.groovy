package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Builder extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def build() {
        if (!context['builder']) {
            context['builder'] = [:]
        }
        // Dispatch the action.
        context << script.drupipeAction([action: "${action.params.builderHandler}.${action.params.builderMethod}"], context)
        context << [returnConfig: true]
    }

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
        context << [returnConfig: true]
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
