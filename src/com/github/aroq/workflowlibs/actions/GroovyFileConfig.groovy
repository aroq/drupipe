package com.github.aroq.workflowlibs.actions

def load(params) {
    if (params.configFileName) {
        params << readGroovyConfig(params.configFileName)
    }
    params
}

def readGroovyConfig(filePath) {
    def text = readFile(filePath)
    groovyConfig(text)
}

@NonCPS
def groovyConfig(text) {
    return new HashMap<>(ConfigSlurper.newInstance().parse(text))
}

