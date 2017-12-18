package com.github.aroq.drupipe.actions

class GroovyFileConfig extends BaseAction {

    def load() {
        def result = [:]
        if (action.params.configFileName && script.fileExists(action.params.configFileName)) {
            result = readGroovyConfig(action.params.configFileName)
        }
        utils.serializeAndDeserialize(result)
    }

    def groovyConfigFromLibraryResource() {
        def config = groovyConfig(script.libraryResource(action.params.resource))
        utils.serializeAndDeserialize(config)
    }

    def readGroovyConfig(filePath) {
        def text = script.readFile(filePath)
        groovyConfig(text)
    }

    @NonCPS
    def groovyConfig(text) {
        new HashMap<>(ConfigSlurper.newInstance(script.env.drupipeEnvironment).parse(text))
    }

}

