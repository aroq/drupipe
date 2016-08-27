package com.github.aroq.workflowlibs.actions

def perform(params) {
    utils = new com.github.aroq.workflowlibs.Utils()

    def config = [:]
    config.workspace = pwd()

    config << params

    if (params.configProviders) {
        echo "Config providers class: ${params.configProviders.getClass()}"
        echo "Config providers size: ${params.configProviders.size()}"
        for (int i = 0; i < params.configProviders.size(); i++) {
            echo "test"
            action = utils.processPipelineAction(params.configProviders[i])
            echo "Action class: ${action.getClass()}"
            params << executeAction(action) {
                p = params
            }
//            if (params.configProviders[i].name == 'docman') {
//                action = utils.processPipelineAction([action: 'Docman.info'])
//                params << executeAction(action) {
//                    p = params
//                }
//            }
        }
    }


    config << params
    config
}

