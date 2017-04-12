package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Config extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def perform() {
        if (context['Config_perform']) {
            return context
        }
        context.workspace = this.script.pwd()

        context.env = this.utils.envToMap()

        context.jenkinsFolderName = this.utils.getJenkinsFolderName(this.script.env.BUILD_URL)
        context.jenkinsJobName = this.utils.getJenkinsJobName(this.script.env.BUILD_URL)

        def providers = [
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

        if (context.configProviders) {
            providers << context.configProviders
        }

        this.script.checkout this.script.scm

        context << context.pipeline.executePipelineActionList(providers, context)
        context << context.env
        context << ['Config_perform': true, returnConfig: true]
        context << action.params.jenkinsParams

        context.environmentParams = [:]
        if (context.environments && context.servers) {
            if (context.environment) {
                def environment = context.environments[context.environment]
                def server = context.servers[environment['server']]
                context.environmentParams = utils.merge(server, environment)
                context.defaultActionParams = utils.merge(context.defaultActionParams, context.environmentParams.defaultActionParams)
                utils.jsonDump(context.environmentParams, 'ENVIRONMENT PARAMS')
            }
        }
    }

    def mothershipConfig() {
        if (this.script.env.MOTHERSHIP_REPO) {
            def sourceObject = [
                name:   'mothershipConfig',
                type:   'git',
                path:   'mothership',
                url:    script.env.MOTHERSHIP_REPO,
                branch: 'master',
            ]

            def providers = [
                [
                    action: 'Source.add',
                    params: [
                        source: sourceObject,
                        credentialsId: context.env.credentialsId,
                    ],
                ],
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'mothershipConfig',
                        configType: 'groovy',
                        configPath: action.params.mothershipConfigFile
                    ]
                ]
            ]
            context << context.pipeline.executePipelineActionList(providers, context)
            def json = this.script.readFile('mothership/projects.json')
            context << this.utils.getMothershipProjectParams(context, json)


        }
        context << [returnConfig: true]
    }

    @NonCPS
    def mergeScenariosConfigs(context, config, sourceName) {
        def scenariosConfig = [:]
        if (config.scenarios) {
            script.echo "Scenarios exists"
            config.scenarios.each { scenario ->
                script.echo "Scenario: ${scenario}"
                def fileName = utils.sourcePath(context, sourceName, "scenarios/${scenario}/config.yaml")
                script.echo "Scenario file name: ${fileName}"
                if (script.fileExists(fileName)) {
                    def scenarioConfig = mergeScenariosConfigs(context, script.readYaml(file: fileName), sourceName)
                    utils.dump(scenarioConfig)
                    scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                }
            }
        }
        utils.merge(scenariosConfig, config)
    }

    def projectConfig() {
        def sourceObject = [
            name: 'projectConfig',
            type: 'dir',
            path: context.docmanConfigPath,
        ]

        def providers = [
            [
                action: 'Source.add',
                params: [source: sourceObject]
            ],
            [
                action: 'Source.loadConfig',
                params: [
                    sourceName: 'projectConfig',
                    configType: 'groovy',
                    configPath: context.docmanConfigFile
                ]
            ],
            [
                action: 'Source.loadConfig',
                params: [
                    sourceName: 'projectConfig',
                    configType: 'yaml',
                    configPath: 'config.yaml'
                ]
            ]
        ]
        context << context.pipeline.executePipelineActionList(providers, context)

        mergeScenariosConfigs(context, context, 'mothershipConfig')

        context << [returnConfig: true]
    }

}
