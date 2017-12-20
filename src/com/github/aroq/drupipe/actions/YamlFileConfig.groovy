package com.github.aroq.drupipe.actions

class YamlFileConfig extends BaseAction {

    def load() {
        def result = [:]
        if (action.params.configFileName && script.fileExists(action.params.configFileName)) {
            result = script.readYaml(file: action.params.configFileName)
        }
        result
    }

    def loadFromLibraryResource() {
        script.readYaml(text: script.libraryResource(action.params.resource))
    }

}

