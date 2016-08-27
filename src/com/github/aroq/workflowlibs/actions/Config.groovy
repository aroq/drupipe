package com.github.aroq.workflowlibs.actions

def perform(params) {

//    def config = [:]
    params.workspace = pwd()
//    config << params

    def providers = []
    source = [name: 'library', type: 'git', path: 'library', url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master']
    providers << [action: 'Source.add', params: [source: source]]
    providers << [action: 'Source.loadConfig', params: [sourceName: 'library', configType: 'groovy', configPath: 'config/config.groovy']]
    providers << params.configProviders
    jsonDump(providers, 'config providers')

    utils = new com.github.aroq.workflowlibs.Utils()
    actionList = utils.processPipelineActionList(providers)
    params << executeActionList(actionList) {
        p = params
    }

//    for (int i = 0; i < providers.size(); i++) {
//        action = utils.processPipelineAction(providers[i])
//        params << executeAction(action) {
//            p = config
//        }
//        config << params
//    }

    params
}

