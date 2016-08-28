package com.github.aroq.workflowlibs.actions

def add(params) {
    def source = params.source
    def result
    dump(params, "source start")
    switch (source.type) {
        case 'git':
            dir(source.path) {
                git url: source.url, branch: source.branch
            }
            result = source.path
            break

        case 'dir':
            result = source.path
            break

        case 'docmanDocroot':
            result = executePipelineAction(action: 'Docman.init', params: [path: 'docroot']) {
                p = params
            }
            break
    }
    if (!params.sources) {
        params.sources = [:]
    }
    if (result) {
        params.sources[source.name] = source
    }
    params.remove('source')
    dump(params, "source end")
    params
}

def loadConfig(params) {
    configFilePath = sourcePath(params, params.sourceName, params.configPath)

    if (params.configType == 'groovy') {
        params << executePipelineAction(action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]) {
            p = params
        }
    }
    params.remove('sourceName')
    params.remove('configPath')
    params.remove('configType')
    params
}
