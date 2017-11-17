package com.github.aroq.drupipe

import com.github.aroq.drupipe.processors.DrupipeProcessor
import com.github.aroq.drupipe.processors.DrupipeProcessorsController

class DrupipeConfig implements Serializable {

    DrupipeController controller

    def script

    def utils

    def config(params) {
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

    DrupipeProcessorsController initProcessorsController(processorsConfig) {
        script.echo "initProcessorsController"
        ArrayList<DrupipeProcessor> processors = []
        for (processorConfig in processorsConfig) {
            script.echo "Processor: ${processorConfig.className}"
            try {
                processors << this.class.classLoader.loadClass("com.github.aroq.drupipe.processors.${processorConfig.className}", true, false)?.newInstance(
                    utils: utils,
                )
                script.echo "Processor: ${processorConfig.className} created"
            }
            catch (err) {
                throw err
            }
        }
        new DrupipeProcessorsController(processors: processors)
    }

    def process() {
        script.echo "DrupipeConfig->process()"
        if (controller.configVersion() > 1) {
            controller.drupipeProcessorsController = initProcessorsController(controller.context.processors)
            controller.context = controller.drupipeProcessorsController.process(controller.context, controller.context, 'context')
            controller.archiveObjectJsonAndYaml(controller.context, 'context_processed')
        }
    }

}
