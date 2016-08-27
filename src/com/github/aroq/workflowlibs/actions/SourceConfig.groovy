package com.github.aroq.workflowlibs.actions

def perform(params) {
    configFilePath = sourcePath(params, params.sourceName, params.sourcePath)
    utils = new com.github.aroq.workflowlibs.Utils()

    if (params.sourceType == 'groovy') {
        params << executeAction(utils.processPipelineAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]])) {
            p = params
        }
    }
    params.remove('sourceName')
    params.remove('path')
    params.remove('type')
    params
}

