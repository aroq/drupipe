package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class YamlDeployer extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def deploy() {
        def deployFile = context.builder.artifactParams.dir + '/' + action.params.deployFile
        if (script.fileExists(deployFile)) {
            def deployYAML = script.readYaml(file: deployFile)
            utils.dump(deployYAML, 'DEPLOY YAML')
            def commands = []
            if (deployYaml.deploy) {
                for (def i=0; i < deployYAML.deploy.size(); i++) {
                    commands << setVariables(deployYaml.deploy[i])
                }
            }
            if (commands) {
                def joinedCommands = commands.join("\n")
                executeCommand(joinedCommands)
            }
        }
        else {
            script.echo "Deploy file ${deployFile} doesn't exist"
        }
    }

    def setVariables(String command) {
        command
    }

    def executeCommand(String command) {
        script.drupipeShell(
            """
            ${command}
            """, context << [shellCommandWithBashLogin: true]
        )

    }
}

