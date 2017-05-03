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
            if (deployYAML.deploy) {
                for (def i = 0; i < deployYAML.deploy.size(); i++) {
                    commands << interpolateCommand(deployYAML.deploy[i])
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

    @NonCPS
    def interpolateCommand(String command) {
        def binding = [context: context, action: action]
        def engine = new groovy.text.SimpleTemplateEngine()
        def template = engine.createTemplate(command).make(binding)
        template.toString()
    }

    def executeCommand(String command) {
        script.drupipeShell(
            """
            ssh ${context.environmentParams.user}@${context.environmentParams.host} ${command}
            """, context
        )

    }
}

