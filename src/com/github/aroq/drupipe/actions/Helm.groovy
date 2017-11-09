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
echo "\${${action.params.access_key_file_id}}" > .google_access_key_file
"""
                , this.action.params
            )
            helmSecretsFile = action.params.workingDir != '.' ? '.google_access_key_file' : '.google_access_key_file'
        }
        else {
            helmSecretsFile = "\${${action.params.access_key_file_id}}"
        }
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

