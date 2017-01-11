package com.github.aroq.drupipe.actions

def add(params) {
    def source = params.source
    def result
    utils = new com.github.aroq.drupipe.Utils()
    switch (source.type) {
        case 'git':
            if (!source.refType) {
                source.refType = 'branch'
            }
            utils.jsonDump(source)
            dir(source.path) {
                deleteDir()
            }
            dir(source.path) {
                if (source.refType == 'branch') {
                    if (params.credentialsID) {
                        echo "With credentials: ${params.credentialsID}"
                        git credentialsId: params.credentialsID, url: source.url, branch: source.branch
                    }
                    else {
                        echo "Without credentials"
                        git url: source.url, branch: source.branch
                    }
                }
            }
            if (source.refType == 'tag') {
                sh "git clone ${source.url} --branch ${source.branch} --depth 1 ${source.path}"
            }
            result = source.path
            break

        case 'dir':
            result = source.path
            break
    }
    if (!params.sources) {
        params.sources = [:]
        params.sourcesList = []
    }
    utils = new com.github.aroq.drupipe.Utils()
    if (result) {
        params.sources[source.name] = new com.github.aroq.drupipe.Source(name: source.name, type: source.type, path: source.path)
        params.sourcesList << params.sources[source.name]

    }
    params.remove('source')
    params << [returnConfig: true]
}

def loadConfig(params) {
    utils = new com.github.aroq.drupipe.Utils()
    if (params.configPath) {
        configFilePath = utils.sourcePath(params, params.sourceName, params.configPath)

        if (params.configType == 'groovy') {
            params << drupipeAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]], params)
        }
        params.remove('sourceName')
        params.remove('configPath')
        params.remove('configType')
    }
    params << [returnConfig: true]
}
