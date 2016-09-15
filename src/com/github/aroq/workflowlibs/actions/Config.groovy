package com.github.aroq.workflowlibs.actions

def perform(params) {
    params.workspace = pwd()

    providers = [
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
    ]

    try {
        if (debug != '0') {
            params.debugEnabled = true
        }
    }
    catch (err) {
    }

    if (params.configProviders) {
        providers << params.configProviders
    }

    params << executePipelineActionList(providers) {
        p = params
    }

    try {
        if (debug != '0') {
            params.debugEnabled = true
        }
    }
    catch (err) {
    }

    configContainer = getConfig()
//    jsonDump(configContainer, 'Config container')
    echo "Config container class:"
    echo(configContainer.getClass())
//    configContainer.addParams(params)

    params << [returnConfig: true]
}

