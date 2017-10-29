package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class YamlFileHandler extends BaseAction {

    def context

    def script

    def utils

    def deployYaml

    def DrupipeActionConroller action

    def init() {
        def repo_url
        if (context.components && context.components['master'] && context.components['master'].repo) {
            repo_url = context.components['master'].repo
        }
        else {
            repo_url = context.configRepo
        }

        def branch
        if (context.environmentParams.git_reference) {
            branch = context.environmentParams.git_reference
        }
        else {
            branch = context.job.branch
        }

        def repoParams = [
            repoAddress: repo_url,
            reference: branch,
            dir: 'docroot',
            repoDirName: 'master',
        ]
        script.drupipeAction([action: "Git.clone", params: repoParams << action.params], context)
    }

    def findDeployYaml() {
        def file
        def files
        files = script.findFiles(glob: "**/.unipipe/${action.params.deployFile}")
        if (files.size() > 0) {
            script.echo files[0].path
            return files[0].path
        }

        files = script.findFiles(glob: "**/.drupipe/${action.params.deployFile}")
        if (files.size() > 0) {
            script.echo files[0].path
            return files[0].path
        }

        files = script.findFiles(glob: "**/${action.params.deployFile}")
        if (files.size() > 0) {
            script.echo files[0].path
            return files[0].path
        }
    }

    def build() {
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
        String deployDir = 'docroot/master'
        if (!script.fileExists(deployDir)) {
            init()
        }
        context['builder']['artifactParams'] = [:]
        context['builder']['artifactParams']['dir'] = '../../' + deployDir
        def deployYamlFile = findDeployYaml()
        if (deployYamlFile && script.fileExists(deployYamlFile)) {
            def deployYAML = script.readYaml(file: deployYamlFile)
            utils.dump(context, deployYAML, 'DEPLOY YAML')
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
                script.echo "No ${stage} defined in ${action.params.deployFile}"
            }
            if (commands) {
                def joinedCommands = commands.join("\n")
                if (stage == 'operations') {
                    executeCommand(joinedCommands)
                }
                else {
                    script.drupipeShell(joinedCommands, action.params)
                }
            }
        }
        else {
            script.echo "Deploy file ${action.params.deployFile} doesn't exist"
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
            """, action.params
        )
    }
}
