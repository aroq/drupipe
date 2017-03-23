package com.github.aroq.drupipe.actions

class GroovyFileConfig extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def load() {
        if (action.params.configFileName && script.fileExists(action.params.configFileName)) {
            context = utils.merge(context, readGroovyConfig(action.params.configFileName))
        }
        context << [returnConfig: true]
    }

    def groovyConfigFromLibraryResource() {
        this.context << groovyConfig(script.libraryResource(action.params.resource))
        this.context << [returnConfig: true]
    }

    def readGroovyConfig(filePath) {
        def text = script.readFile(filePath)
        groovyConfig(text)
    }

    @NonCPS
    def groovyConfig(text) {
        return new HashMap<>(ConfigSlurper.newInstance(script.env.drupipeEnvironment).parse(text))
    }

}

