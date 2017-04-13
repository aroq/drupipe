package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class GroovyFileConfig extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def load() {
        def result = [:]
        if (action.params.configFileName && script.fileExists(action.params.configFileName)) {
            result = utils.merge(context, readGroovyConfig(action.params.configFileName))
        }
        result
    }

    def groovyConfigFromLibraryResource() {
        groovyConfig(script.libraryResource(action.params.resource))
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

