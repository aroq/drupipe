package com.github.aroq.workflowlibs.actions

def perform(params) {
    utils = new com.github.aroq.workflowlibs.Utils()

    def config = [:]
    config.workspace = pwd()
    config << params

    def providers = []
    providers << [action: 'Library.perform', params: []]
    providers << [action: 'GroovyFileConfig.perform', params: [configFileName: 'library/config/config.groovy']]
    providers << params.configProviders

    dump(config, "before action list")
    for (int i = 0; i < providers.size(); i++) {
        action = utils.processPipelineAction(providers[i])
        dump(config, "before action list" + i.toString())
        params << executeAction(action) {
            p = params
        }
        dump(config, "after action list" + i.toString())
    }
    dump(config, "after action list")
    config << params
    dump(config, "before return")

    config
}

