package com.github.aroq.workflowlibs.actions

def perform(params) {
    actions = [
        [
            action: 'Source.add',
            params: [
                source: [
                    name: 'docmanConfig',
                    type: 'git',
                    url: config_repo,
                    path: 'docroot/config',
                    branch: 'master',
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
    ]

    params << executePipelineActionList(actions) {
        p = params
    }

    params << [returnConfig: true]
}

