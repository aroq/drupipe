package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class YamlFileConfig extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def load() {
        def result = [:]
        if (action.params.configFileName && script.fileExists(action.params.configFileName)) {
            result = utils.merge(context, script.readYaml(file: action.params.configFileName))
        }
        result
    }

}

