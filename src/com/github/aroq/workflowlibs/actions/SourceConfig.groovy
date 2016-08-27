package com.github.aroq.workflowlibs.actions

def load(params) {
    configFilePath = sourcePath(params, params.sourceName, params.configPath)
    utils = new com.github.aroq.workflowlibs.Utils()

    if (params.configType == 'groovy') {
        params << executeAction(utils.processPipelineAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]])) {
            p = params
        }
    }
    params.remove('sourceName')
    params.remove('configPath')
    params.remove('configType')
    params
}

