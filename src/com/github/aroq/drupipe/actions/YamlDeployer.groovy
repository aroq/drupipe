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
        }
        else {
            script.echo "Deploy file ${deployFile} doesn't exist"
        }
    }
}

