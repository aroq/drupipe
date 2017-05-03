package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class YamlDeployer extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def deploy() {
        if (script.fileExists(action.params.deployFile)) {
            def deployYAML = readYaml(file: action.params.deployFile)
            utils.dump(deployYAML, 'DEPLOY YAML')
        }
        else {
            script.echo "Deploy file ${action.params.deployFile} doesn't exist"
        }
    }
}

