package com.github.aroq.drupipe.actions

class Helm extends BaseShellAction {

    def apply_hook_pre_params() {
        action.pipeline.drupipeLogger.trace "Inside appy_hook_preprocess()"
        if (action.pipeline.context.containerMode == 'kubernetes') {
            this.script.drupipeShell("""
echo "${action.params.secret_values_file}" > .secret_values_file_id
""", this.action.params
            )
            action.params.secret_values_file = '.secret_values_file_id'
        }
    }

    def apply_hook_pre_process() {
        def files = []
        for (fileName in action.params.flags['-f']) {
//            fileName = fileName - 'charts/'
            if (script.fileExists(fileName)) {
                files.add fileName
            }
            else {
                action.pipeline.drupipeLogger.warning "Helm values file does not exists: ${fileName}"
            }
        }
        action.params.flags['-f'] = files

    }

//    def status_hook_post_process() {
//        if (!action.params.containsKey('charts_dir')) {
//            action.params.namespace = action.params.namespace.replaceAll('/', '-')
//            action.params.release_name = action.params.release_name.replaceAll('/', '-')
//        }
//        else {
//            action.params.chart = action.params.charts_dir + '/' + action.params.chart
//        }
//    }
//
//    def delete_hook_post_process() {
//        if (!action.params.containsKey('charts_dir')) {
//            action.params.namespace = action.params.namespace.replaceAll('/', '-')
//            action.params.release_name = action.params.release_name.replaceAll('/', '-')
//        }
//        else {
//            action.params.chart = action.params.charts_dir + '/' + action.params.chart
//        }
//    }
//
//    def apply_hook_post_process() {
//        if (!action.params.containsKey('charts_dir')) {
//            action.params.namespace = action.params.namespace.replaceAll('/', '-')
//            action.params.release_name = action.params.release_name.replaceAll('/', '-')
//            action.params.values_file = action.params.values_file.replaceAll('/', '-')
//        }
//        else {
//            action.params.chart = action.params.charts_dir + '/' + action.params.chart
//        }
//    }

//    def apply_hook_final_process() {
//    }

}

