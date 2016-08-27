package com.github.aroq.workflowlibs.actions

def perform(params) {
    utils = new com.github.aroq.workflowlibs.Utils()

    def config = [:]
    config.workspace = pwd()

    config << params

    def providers = [:]
    providers << [action: 'Library.perform', params: [configFileName: 'library/config/docroot.config']]
    providers << [action: 'GroovyFileConfig.perform', params: [configFileName: 'library/config/docroot.config']]
    providers << params.configProviders

    for (int i = 0; i < providers.size(); i++) {
        action = utils.processPipelineAction(providers[i])
        params << executeAction(action) {
            p = params
        }
    }

    config << params
    config
}

