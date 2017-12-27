package com.github.aroq.drupipe.actions

class Helm extends BaseShellAction {

    def apply_hook_pre_params() {
        if (action.pipeline.context.containerMode == 'kubernetes') {
            if (action.params.secret_values_file_id) {
                this.script.drupipeShell("""echo "\$${action.params.secret_values_file_id}" > .secret_values_file_id""", this.action.params)
            }
            action.params.secret_values_file = '.secret_values_file_id'
        }
        else {
            action.params.secret_values_file = action.pipeline.context.env[action.params.secret_values_file_id]
        }
    }

    def apply_hook_post_params() {
        def files = []
        for (fileName in action.params.flags['-f']) {
            if (script.fileExists(fileName)) {
                files.add fileName
            }
            else {
                action.pipeline.drupipeLogger.warning "Helm values file does not exists: ${fileName}"
            }
        }
        action.params.flags['-f'] = files
    }

}
