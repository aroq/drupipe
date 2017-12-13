package com.github.aroq.drupipe

class DrupipeSourcesController implements Serializable {

    LinkedHashMap loadedSources

    ArrayList sourcesList

    def script

    com.github.aroq.drupipe.Utils utils

    DrupipeController controller

    def sourceAdd(source) {
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
                        if (source.credentialsId) {
                            this.script.echo "With credentials: ${source.credentialsId}"
                            this.script.git credentialsId: source.credentialsId, url: source.url, branch: source.branch
                        } else {
                            this.script.echo "Without credentials"
                            this.script.git url: source.url, branch: source.branch
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
        if (!loadedSources) {
            loadedSources = [:]
            sourcesList = []
        }
        if (path) {
            loadedSources[source.name] = [name: source.name, type: source.type, path: source.path]
            sourcesList << loadedSources[source.name]
        }

        controller.drupipeLogger.log "Source is added: ${source.name}"

        result
    }

    def sourceLoad(params) {
        def result = [:]
        if (params.configPath) {
            def configFilePath = sourcePath(controller.context, params.sourceName, params.configPath)
            if (configFilePath) {
                if (script.fileExists(configFilePath)) {
                    if (params.configType == 'groovy') {
                        result = utils.groovyFileLoad(configFilePath)
//                        result = script.drupipeAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]], controller)
                    }
                    else if (params.configType == 'yaml') {
                        result = utils.yamlFileLoad(configFilePath)
//                        result = script.drupipeAction([action: 'YamlFileConfig.load', params: [configFileName: configFilePath]], controller)
                    }
                }
                controller.drupipeLogger.log "Source file is loaded: ${configFilePath}"
            }
            else {
                script.echo "Config file doesn't exists: ${configFilePath}"
            }
        }

        result
    }

    def sourcePath(params, sourceName, String path) {
        controller.drupipeLogger.debugLog(params, sourceName, 'Source name')
        if (sourceName in loadedSources) {
            loadedSources[sourceName].path + '/' + path
        }
    }

    def sourceDir(params, sourceName) {
        controller.drupipeLogger.debugLog(params, sourceName, 'Source name')
        if (sourceName in loadedSources) {
            loadedSources[sourceName].path
        }
    }


}
