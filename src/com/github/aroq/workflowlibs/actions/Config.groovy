package com.github.aroq.workflowlibs.actions

def perform(params) {
    params.workspace = pwd()

    def providers = [
        [
            action: 'Source.add',
            params: [
                source: [
                    name: 'library',
                    type: 'git',
                    url: 'https://github.com/aroq/jenkins-pipeline-library.git',
                    path: 'library',
                    branch: 'master',
                ]
            ]
        ],
        [
            action: 'Source.loadConfig',
            params: [
                sourceName: 'library',
                configType: 'groovy',
                configPath: 'config/config.groovy'
            ]
        ],
    ] << params.configProviders
//    source = [name: 'library', type: 'git', path: 'library', url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master']
//    providers << [action: 'Source.add', params: [source: source]]
//    providers << [action: 'Source.loadConfig', params: [sourceName: 'library', configType: 'groovy', configPath: 'config/config.groovy']]
//    providers << params.configProviders
    jsonDump(providers, 'config providers')

    params << executePipelineActionList(providers) {
        p = params
    }

    params
}

