package com.github.aroq.drupipe

import com.github.aroq.drupipe.processors.DrupipeProcessor
import com.github.aroq.drupipe.processors.DrupipeProcessorsController

class DrupipeConfig implements Serializable {

    def controller

    def script

    def utils

    def config(params) {
        script.node('master') {
//            utils.log "Executing pipeline"
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

    DrupipeProcessorsController initProcessorsController(parent, processorsConfig) {
//        utils.log "initProcessorsController"
        ArrayList<DrupipeProcessor> processors = []
        for (processorConfig in processorsConfig) {
            utils.log "Processor: ${processorConfig.className}"
            try {
                def properties = [utils: utils]
                if (processorConfig.properties) {
                    properties << processorConfig.properties
                }
                processors << parent.class.classLoader.loadClass("com.github.aroq.drupipe.processors.${processorConfig.className}", true, false)?.newInstance(
                    properties
                )
                utils.log "Processor: ${processorConfig.className} created"
            }
            catch (err) {
                throw err
            }
        }
        new DrupipeProcessorsController(processors: processors)
    }

    def processItem(item, parentKey, paramsKey = 'params', mode) {
        utils.log "DrupipeConfig->processItem BEFORE serializeAndDeserialize"
        def tempContext = utils.serializeAndDeserialize(controller.context)
        utils.log "DrupipeConfig->processItem AFTER serializeAndDeserialize"
        controller.drupipeProcessorsController.process(controller.context, item, parentKey, paramsKey, mode)
    }

    def process() {
        utils.log "DrupipeConfig->process()"
        if (controller.configVersion() > 1) {
            controller.drupipeProcessorsController = initProcessorsController(this, controller.context.processors)
            controller.context.jobs = processItem(controller.context.jobs, 'context', 'params', 'config')
            controller.archiveObjectJsonAndYaml(controller.context, 'context_processed')
        }
    }

}
