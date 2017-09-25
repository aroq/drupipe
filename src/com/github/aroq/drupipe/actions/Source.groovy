package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Source extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def add() {
        def source = this.action.params.source
        def result
        switch (source.type) {
            case 'git':
                if (!source.refType) {
                    source.refType = 'branch'
                }
                this.script.dir(source.path) {
                    this.script.deleteDir()
                }

                source.mode = source.mode ? source.mode : 'pipeline'

                if (source.refType == 'branch' && source.mode == 'pipeline') {
                    this.script.dir(source.path) {
                        if (this.action.params.credentialsId) {
                            this.script.echo "With credentials: ${this.action.params.credentialsId}"
                            this.script.git credentialsId: this.action.params.credentialsId, url: source.url, branch: source.branch
                        } else {
                            this.script.echo "Without credentials"
                            this.script.git url: source.url, branch: source.branch
                        }
                    }
                }
//                else if (source.refType == 'branch' && source.mode == 'shell') {
//                    this.script.sh "git clone ${source.url} --branch ${source.branch} --depth 1 ${source.path}"
//                }
                else {
                    this.script.drupipeShell("git clone ${source.url} --branch ${source.branch} --depth 1 ${source.path}", context)
                }
                result = source.path
                break

            case 'dir':
                result = source.path
                break
        }
        if (!context.loadedSources) {
            context.loadedSources = [:]
            context.sourcesList = []
        }
        if (result) {
            context.loadedSources[source.name] = new com.github.aroq.drupipe.DrupipeSource(name: source.name, type: source.type, path: source.path)
            context.sourcesList << context.loadedSources[source.name]
            utils.debugLog(context, context.loadedSources, "Loaded sources (after Source.add)", [debugMode: 'json'])
        }
        [:]
    }

    def loadConfig() {
        def result = [:]
        if (action.params.configPath) {
            def configFilePath = utils.sourcePath(context, action.params.sourceName, action.params.configPath)

            if (script.fileExists(configFilePath)) {
                if (action.params.configType == 'groovy') {
                    result = this.script.drupipeAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]], context)
                }
                else if (action.params.configType == 'yaml') {
                    result = this.script.drupipeAction([action: 'YamlFileConfig.load', params: [configFileName: configFilePath]], context)
                }
            }

            result.remove('sourceName')
            result.remove('configPath')
            result.remove('configType')
        }
        result
    }
}

