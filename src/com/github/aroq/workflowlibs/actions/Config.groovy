package com.github.aroq.workflowlibs.actions
import groovy.json.JsonSlurper

def perform(commandParams) {
    if (commandParams['Config_perform']) {
      return commandParams
    }
    commandParams.workspace = pwd()
    utils = new com.github.aroq.workflowlibs.Utils()

    commandParams.env = utils.envToMap()

    commandParams.jenkinsFolderName = utils.getJenkinsFolderName(env.BUILD_URL)
    commandParams.jenkinsJobName = utils.getJenkinsJobName(env.BUILD_URL)

    providers = [
        [
            action: 'GroovyFileConfig.groovyConfigFromLibraryResource', params: [resource: 'com/github/aroq/drupipe/config.groovy']
        ],
        [
            action: "Config.mothershipConfig"
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

def mothershipConfig(commandParams) {
    if (env.MOTHERSHIP_REPO) {
        sourceObject = [
            name: 'mothershipConfig',
            type: 'git',
            path: 'mothership',
            url: env.MOTHERSHIP_REPO,
            branch: 'master',
        ]

        providers = [
            [
                action: 'Source.add',
                params: [
                    source: sourceObject,
                    credentialsID: 'zebra',
                ],
            ],
            [
                action: 'Source.loadConfig',
                params: [
                    sourceName: 'mothershipConfig',
                    configType: 'groovy',
                    configPath: commandParams.mothershipConfigFile
                ]
            ]
        ]

        commandParams << executePipelineActionList(providers) {
            p = commandParams
        }
        utils = new com.github.aroq.workflowlibs.Utils()
        def json = readFile('mothership/projects.json')
        commandParams << utils.getMothershipProjectParams(commandParams, json)
    }
    commandParams << [returnConfig: true]
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
