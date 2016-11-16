package com.github.aroq.workflowlibs.actions

def perform(commandParams) {
    if (commandParams['Config_perform']) {
      return commandParams
    }
    commandParams.workspace = pwd()

    providers = [
        [
            action: 'GroovyFileConfig.groovyConfigFromLibraryResource', params: [resource: 'com/github/aroq/drupipe/config.groovy']
        ],
        [
            action: "Config.projectConfig"
        ],
    ]

    if (commandParams.configProviders) {
        providers << commandParams.configProviders
    }

    checkout scm

    commandParams << executePipelineActionList(providers) {
        p = commandParams
    }

    commandParams << ['Config_perform': true, returnConfig: true]
}

def projectConfig(commandParams) {
    sourceObject = [
        name: 'projectConfig',
        type: 'dir',
        path: commandParams.projectConfigPath,
    ]

    providers = [
        [
            action: 'Source.add',
            params: [source: sourceObject]
        ],
        [
            action: 'Source.loadConfig',
            params: [
                sourceName: 'projectConfig',
                configType: 'groovy',
                configPath: commandParams.projectConfigFile
            ]
        ]
    ]

    commandParams << executePipelineActionList(providers) {
        p = commandParams
    }

    commandParams << [returnConfig: true]
}
