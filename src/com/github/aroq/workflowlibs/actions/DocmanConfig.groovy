package com.github.aroq.workflowlibs.actions

def perform(params) {
    actions = [
        [
            action: 'Source.add',
            params: [
                source: [
                    name: 'docmanConfig',
                    type: 'git',
                    path: 'docroot/config',
                ]
            ]
        ],
        [
            action: 'Config.perform',
            params: [
                configProviders: [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'docmanConfig',
                        configType: 'groovy',
                        configPath: 'docroot.config'
                    ]
                ]
            ]
        ],
        [
            action: 'Docman.info',
        ],
    ]
    params << executePipelineActionList(actions) {
        p = params
    }

    params
}

