package com.github.aroq.drupipe.actions

class Builder extends BaseAction {

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

}

