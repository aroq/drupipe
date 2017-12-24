package com.github.aroq.drupipe.actions

class Helm extends BaseShellAction {

    def apply_hook_pre_params() {
        action.pipeline.drupipeLogger.trace "Inside appy_hook_preprocess()"
        if (action.pipeline.context.containerMode == 'kubernetes') {
            this.script.drupipeShell("""echo "\$${action.params.secret_values_file_id}" > .secret_values_file_id""", this.action.params)
            action.params.secret_values_file = '.secret_values_file_id'
        }
    }

    def apply_hook_pre_process() {
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

