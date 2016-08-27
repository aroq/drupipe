package com.github.aroq.workflowlibs.actions

def perform(params) {
    def config = [:]
    config.workspace = pwd()

    config << params

    if (config.configProvider == 'docman') {
//        params << executeAction(action) {
//            p = params
//        }
        def docman = new com.github.aroq.workflowlibs.actions.Docman()
        docman.info(config)
    }

    if (params.configFileName) {
        config = readGroovyConfig(params.configFileName)
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

