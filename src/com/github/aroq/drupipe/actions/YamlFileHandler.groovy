package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class YamlFileHandler extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def build() {
        def repo_url
        if (context.components && context.components['master'] && context.components['master'].repo) {
            repo_url = context.components['master'].repo
        }
        else {
            repo_url = context.configRepo
        }

        def repoParams = [
            repoAddress: repo_url,
            reference: context.environmentParams.git_reference,
            dir: 'docroot',
            repoDirName: 'master',
        ]
        script.drupipeAction([action: "Git.clone", params: repoParams << action.params], context)

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
        String deployDir = context.builder ? context.builder.artifactParams.dir : 'docroot/master'
        context['builder']['artifactParams'] = [:]
        context['builder']['artifactParams']['dir'] = deployDir
        String deployFile = deployDir + '/' + action.params.deployFile
        if (script.fileExists(deployFile)) {
            def deployYAML = script.readYaml(file: deployFile)
            utils.dump(deployYAML, 'DEPLOY YAML')
            def commands = []
            if (stage == 'operations') {
                def root = context.environmentParams.root
                root = root.substring(0, root.length() - (root.endsWith("/") ? 1 : 0))
                commands << "cd ${root}"
                commands << "pwd"
                commands << "ls -lah"
            }
            else {
                commands << "cd ${deployDir}"
                commands << "pwd"
                commands << "ls -lah"
            }
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
                if (stage == 'operations') {
                    executeCommand(joinedCommands)
                }
                else {
                    script.drupipeShell(joinedCommands, context)
                }
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
