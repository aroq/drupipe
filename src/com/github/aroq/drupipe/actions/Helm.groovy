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

    def hook_preprocess() {
        script.echo "Inside hook_preprocess()"
        if (action.pipeline.context.containerMode == 'kubernetes') {
            this.script.drupipeShell("""

echo "\${${action.params.secret_values_file_id}}" > secret_values_file_id.

""", this.action.params

            )
            action.params.secret_values_file = action.params.workingDir != '.' ? '.secret_values_file_id' : '.secret_values_file_id'
        }
    }

    def apply() {
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

