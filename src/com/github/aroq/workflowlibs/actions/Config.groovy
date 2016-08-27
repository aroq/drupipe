package com.github.aroq.workflowlibs.actions

def perform(params) {
    utils = new com.github.aroq.workflowlibs.Utils()

    def config = [:]
    config.workspace = pwd()

    config << params

    for (int i = 0; i < params.configProviders.size(); i++) {
        if (configProviders[i].name == 'docman') {
            action = utils.processPipelineAction([action: 'Docman.info'])
            params << executeAction(action) {
                p = params
            }
        }
    }

    if (params.configFileName) {
        config << readGroovyConfig(params.configFileName)
    }

    config << params
    config
}

def readGroovyConfig(filePath) {
    def text = readFile(filePath)
    groovyConfig(text)
}

@NonCPS
def groovyConfig(text) {
    return new HashMap<>(ConfigSlurper.newInstance().parse(text))
}

