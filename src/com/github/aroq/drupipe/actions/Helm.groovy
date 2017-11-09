package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Helm extends BaseAction {

    def context

    def script

    def utils

    DrupipeActionWrapper action

    def init() {
        executeHelmCommand()
    }

    def apply() {
        String helmSecretsFile
        if (action.pipeline.context.containerMode == 'kubernetes') {
            this.script.drupipeShell(
                """
echo "\${${action.params.secret_values_file_id}}" > .google_access_key_file
"""
                , this.action.params
            )
            action.params.secret_values_file = action.params.workingDir != '.' ? '.secret_values_file_id' : '.secret_values_file_id'
        }
//        else {
//            helmSecretsFile = "\${${action.params.secret_values_file_id}}"
//        }
        executeHelmCommand()
    }

    def status() {
        executeHelmCommand()
    }

    def delete() {
        executeHelmCommand()
    }

    def executeHelmCommand() {
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

