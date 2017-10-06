package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class YamlFileHandler extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def build() {
        if (context.components && context.components['master'] && context.components['master'].repo) {
            def repoParams = [
                repoAddress: context.components['master'].repo,
                reference: context.environmentParams.git_reference,
                dir: 'docroot',
                repoDirName: 'master',
            ]
            script.drupipeAction([action: "Git.clone", params: repoParams << action.params], context)
        }

        process('build')
    }

    def deploy() {
        process('deploy')
    }

    def operations() {
        process('operations')
    }

    def test() {
        process('test')
    }

    def process(String stage) {
        utils.dump(context, 'YamlFileHandler process context')
        script.drupipeShell('pwd && ls -lah', context)
        executeCommand('pwd && ls -lah')
        String deployFile = context.builder ? context.builder.artifactParams.dir + '/' + action.params.deployFile : 'docroot/master/' + action.params.deployFile
        if (script.fileExists(deployFile)) {
            def deployYAML = script.readYaml(file: deployFile)
            utils.dump(deployYAML, 'DEPLOY YAML')
            def commands = []
            if (deployYAML[stage]) {
                for (def i = 0; i < deployYAML[stage].size(); i++) {
                    commands << interpolateCommand(deployYAML[stage][i])
                }
            }
            else {
                script.echo "No ${stage} defined in ${deployFile}"
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
            ssh ${context.environmentParams.user}@${context.environmentParams.host} "${command}"
            """, context
        )
    }
}
