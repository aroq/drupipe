package com.github.aroq.drupipe

class DrupipeConfig implements Serializable {

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

    def process() {
        if (controller.configVersion() > 1) {
            controller.context = controller.drupipeProcessorsController.process(controller.context, controller.context, 'context')
            controller.archiveObjectJsonAndYaml(controller.context, 'context_processed')
        }
    }

}
