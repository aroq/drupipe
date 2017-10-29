package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Source extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def add() {
        def source = this.action.params.source
        def result = [:]
        def path
        switch (source.type) {
            case 'git':
                if (!source.refType) {
                    source.refType = 'branch'
                }
                script.dir(source.path) {
                    this.script.deleteDir()
                }

                source.mode = source.mode ? source.mode : 'pipeline'

                if (source.refType == 'branch' && source.mode == 'pipeline') {
                    script.dir(source.path) {
                        if (this.action.params.credentialsId) {
                            script.echo "With credentials: ${this.action.params.credentialsId}"
                            script.git credentialsId: this.action.params.credentialsId, url: source.url, branch: source.branch
                        } else {
                            script.echo "Without credentials"
                            script.git url: source.url, branch: source.branch
                        }
                    }
                }
                else {
                    script.sh "git clone ${source.url} --branch ${source.branch} --depth 1 ${source.path}"
                }
                path = source.path
                break

            case 'dir':
                path = source.path
                break
        }
        if (!result.loadedSources) {
            result.loadedSources = [:]
            result.sourcesList = []
        }
        if (path) {
            result.loadedSources[source.name] = new com.github.aroq.drupipe.DrupipeSource(name: source.name, type: source.type, path: source.path)
            result.sourcesList << result.loadedSources[source.name]
        }
        result
    }

    def loadConfig() {
        def result = [:]
        if (action.params.configPath) {
            def configFilePath = utils.sourcePath(action.pipeline.context, action.params.sourceName, action.params.configPath)
            if (configFilePath) {
                if (script.fileExists(configFilePath)) {
                    if (action.params.configType == 'groovy') {
                        result = script.drupipeAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]], action.pipeline)
                    }
                    else if (action.params.configType == 'yaml') {
                        result = script.drupipeAction([action: 'YamlFileConfig.load', params: [configFileName: configFilePath]], action.pipeline)
                    }
                }
            }
            else {
                 script.echo "Config file doesn't exists: ${configFilePath}"
            }

        }
        result
    }
}

