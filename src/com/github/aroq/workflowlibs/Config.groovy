package com.github.aroq.workflowlibs

def perform(params) {
    def config = [:]
    if (params.configFileName) {
        config = readGroovyConfig(configPath + '/' + params.configFileName)
    }
    config.workspace = pwd()
    config << params

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        config.initDocman = false
        docman.info(config)
    }

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

