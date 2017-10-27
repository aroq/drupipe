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
        def result = [:]
        def configFileYamlPath = '.unipipe/temp/groovy.file.config.yaml'
        def config = groovyConfig(script.libraryResource(action.params.resource))
        if (config) {
            if (this.script.fileExists(configFileYamlPath)) {
                this.script.sh("rm -f ${configFileYamlPath}")
            }
            this.script.writeYaml(file: configFileYamlPath, data: config)
            if (this.context.pipeline.script.fileExists(configFileYamlPath)) {
                result = this.context.pipeline.script.readYaml(file: configFileYamlPath)
            }
            utils.debugLog(result, result, "GroovyFileConfig.RESULT", [debugMode: 'json'], [], true)
        }
        result
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

