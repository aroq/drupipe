package com.github.aroq.workflowlibs.actions

def perform(params) {
    utils = new com.github.aroq.workflowlibs.Utils()

    def config = [:]
    config.workspace = pwd()

    config << params

    if (config.configProvider == 'docman') {
        action = utils.processPipelineAction([action: 'Docman.info'])
        params << executeAction(action) {
            p = params
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

