package com.github.aroq.drupipe

class DrupipeConfig {

    DrupipeController controller

    def script

    def utils

    def config(params) {
        utils = controller.utils
        script.node('master') {
            script.echo "Executing pipeline"
            params.debugEnabled = params.debugEnabled && params.debugEnabled != '0' ? true : false
            utils.dump(params, params, 'PIPELINE-PARAMS')
            // Get config (context).
            script.drupipeAction([action: 'Config.perform', params: [jenkinsParams: params]], controller)
            controller.archiveObjectJsonAndYaml(controller.context, 'context')
        }
    }

    int configVersion() {
        controller.context.config_version as int
    }
}
