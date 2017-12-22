package com.github.aroq.drupipe.actions

class Helm extends BaseShellAction {

    def chart_dir_hook_pre_process() {
        apply_hook_pre_process()
    }

    def apply_hook_pre_process() {
        action.pipeline.drupipeLogger.trace "Inside appy_hook_preprocess()"
        if (action.pipeline.context.containerMode == 'kubernetes') {
            this.script.drupipeShell("""
echo "\${${action.params.secret_values_file_id}}" > .secret_values_file_id
""", this.action.params
            )
            action.params.secret_values_file = action.params.workingDir != '.' ? '.secret_values_file_id' : '.secret_values_file_id'
        }
    }

    def status_hook_post_process() {
        action.params.namespace = action.params.namespace.replaceAll('/', '-')
        action.params.release_name = action.params.release_name.replaceAll('/', '-')
    }

    def delete_hook_post_process() {
        action.params.namespace = action.params.namespace.replaceAll('/', '-')
        action.params.release_name = action.params.release_name.replaceAll('/', '-')
    }

    def chart_dir_hook_post_process() {
        apply_hook_post_process()
    }

    def apply_hook_post_process() {
        action.params.namespace = action.params.namespace.replaceAll('/', '-')
        action.params.release_name = action.params.release_name.replaceAll('/', '-')
        action.params.values_file = action.params.values_file.replaceAll('/', '-')

        def files = []
        for (fileName in action.params.flags['-f']) {
            if (script.fileExists(fileName)) {
                files.add fileName
            }
        }
        action.params.flags['-f'] = files

        action.pipeline.drupipeLogger.debugLog(action.params, action.params, "action.params in Helm.apply_hook_preprocess()", [debugMode: 'json'])
    }

}

