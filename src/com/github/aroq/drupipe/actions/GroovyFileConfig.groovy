package com.github.aroq.drupipe.actions

def load(params) {
    if (params.configFileName && fileExists(params.configFileName)) {
        params << readGroovyConfig(params.configFileName)
    }
    params << [returnConfig: true]
}

def readGroovyConfig(filePath) {
    def text = readFile(filePath)
    groovyConfig(text)
}

def groovyConfigFromLibraryResource(params) {
    params << groovyConfig(libraryResource(params.resource))
    params << [returnConfig: true]
}

@NonCPS
def groovyConfig(text) {
    return new HashMap<>(ConfigSlurper.newInstance(env.drupipeEnvironment).parse(text))
}

