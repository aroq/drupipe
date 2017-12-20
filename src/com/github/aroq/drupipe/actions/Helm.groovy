package com.github.aroq.drupipe.actions

class Helm extends BaseShellAction {

    def apply_hook_preprocess() {
        action.pipeline.drupipeLogger.trace "Inside appy_hook_preprocess()"
        if (action.pipeline.context.containerMode == 'kubernetes') {
            this.script.drupipeShell("""
echo "\${${action.params.secret_values_file_id}}" > .secret_values_file_id
""", this.action.params
            )
            action.params.secret_values_file = action.params.workingDir != '.' ? '.secret_values_file_id' : '.secret_values_file_id'
        }
    }

}

