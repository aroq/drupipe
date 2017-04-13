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
        context << script.drupipeAction([action: "${action.params.buildHandler.handler}.${action.params.buildHandler.method}"], context)
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

//    def retrieveArtifact() {
//        if (!context['builder']) {
//            context['builder'] = [:]
//        }
//        script.drupipeAction([action: "${action.params.buildHandler.handler}.artifactParams"], context)
//        context << script.drupipeAction([action: "${action.params.artifactHandler.handler}.${action.params.artifactHandler.method}", params: context.builder.artifactParams], context)
//        context.projectName = 'master'
//        context << [returnConfig: true]
//    }

}
