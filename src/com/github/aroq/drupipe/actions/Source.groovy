package com.github.aroq.drupipe.actions

class Source extends BaseAction {
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
                this.script.dir(source.path) {
                    if (source.refType == 'branch') {
                        if (this.action.params.credentialsId) {
                            this.script.echo "With credentials: ${this.action.params.credentialsId}"
                            this.script.git credentialsId: this.action.params.credentialsId, url: source.url, branch: source.branch
                        }
                        else {
                            this.script.echo "Without credentials"
                            this.script.git url: source.url, branch: source.branch
                        }
                    }
                }
                if (source.refType == 'tag') {
                    script.sh "git clone ${source.url} --branch ${source.branch} --depth 1 ${source.path}"
                }
                result = source.path
                break

            case 'dir':
                result = source.path
                break
        }
        if (!context.sources) {
            context.sources = [:]
            context.sourcesList = []
        }
        if (result) {
            context.sources[source.name] = new com.github.aroq.drupipe.DrupipeSource(name: source.name, type: source.type, path: source.path)
            context.sourcesList << context.sources[source.name]

        }
        context.remove('source')
        context << [returnConfig: true]
    }

    def loadConfig() {
        if (action.params.configPath) {
            def configFilePath = utils.sourcePath(context, action.params.sourceName, action.params.configPath)

            if (action.params.configType == 'groovy') {
                context << this.script.drupipeAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]], context)
            }
            context.remove('sourceName')
            context.remove('configPath')
            context.remove('configType')
        }
        context << [returnConfig: true]
    }
}

