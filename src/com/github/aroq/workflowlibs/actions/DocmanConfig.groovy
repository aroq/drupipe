package com.github.aroq.workflowlibs.actions

def perform(params) {
    actions = [
        [
            action: 'Source.add',
            params: [
                source: [
                    name: 'docmanDocroot',
                    type: 'docmanDocroot',
                    path: 'docroot',
                ]
            ]
        ],
        [
            action: 'Config.perform',
            params: [
                configProviders: [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'docmanDocroot',
                        configType: 'groovy',
                        configPath: 'config/docroot.config'
                    ]
                ]
            ]
        ],
        [
            action: 'Docman.info',
        ],
    ]
    executePipelineActionList(actions) {
        p = params
    }

    params
}

