package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Config extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def configRepo

    def perform() {
        if (context['Config_perform']) {
            return context
        }

        def providers = [
            [
                action: 'GroovyFileConfig.groovyConfigFromLibraryResource', params: [resource: 'com/github/aroq/drupipe/config.groovy']
            ],
            [
                action: "Config.envConfig"
            ],
            [
                action: "Config.mothershipConfig"
            ],
            [
                action: "Config.projectConfig"
            ],
            [
                action: "Config.jenkinsConfig"
            ],
        ]

        if (context.configProviders) {
            providers << context.configProviders
        }

        this.script.checkout this.script.scm

        context << context.pipeline.executePipelineActionList(providers, context)

//        if (!context.environments) {
//            throw new RuntimeException('No context.environments defined')
//        }
//
//        if (!context.servers) {
//            throw new RuntimeException('No context.servers defined')
//        }

        context.environmentParams = [:]
        if (context.environments) {
            if (context.environment) {
                def environment = context.environments[context.environment]
                if (context.servers) {
                    def server = context.servers[environment['server']]
                    context.environmentParams = utils.merge(server, environment)
                }
                else {
                    context.environmentParams = environment
                }
                context.defaultActionParams = utils.merge(context.defaultActionParams, context.environmentParams.defaultActionParams)
                utils.jsonDump(context.environmentParams, 'ENVIRONMENT PARAMS')
            }
        }

        context.drupipeShellReturnStdout = false

        def stashes = context.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')

        script.echo "Stashes: ${stashes}"

        script.stash name: 'config', includes: "${stashes}", excludes: ".git, .git/**"
        context
    }

    def jenkinsConfig() {
        action.params.jenkinsParams
    }

    def envConfig() {
        def result = [:]
        result.workspace = this.script.pwd()
        result.env = this.utils.envToMap()
        result << result.env

        String jobPath = this.script.env.BUILD_URL ? this.script.env.BUILD_URL : this.script.env.JOB_DISPLAY_URL

        result.jenkinsFolderName = this.utils.getJenkinsFolderName(jobPath)
        result.jenkinsJobName = this.utils.getJenkinsJobName(jobPath)

        if (this.script.env.KUBERNETES_PORT) {
            result.containerMode = 'kubernetes'
        }

        result
    }

    def mothershipConfig() {
        def result
        if (this.script.env.MOTHERSHIP_REPO) {
            def sourceObject = [
                name:   'mothership',
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
                        sourceName: 'mothership',
                        configType: 'groovy',
                        configPath: action.params.mothershipConfigFile
                    ]
                ]
            ]
            result = context.pipeline.executePipelineActionList(providers, context)
            def mothershipConfigFileContent = this.utils.getMothershipConfigFile(context)
            this.script.echo("mothershipConfigFileContent: ${mothershipConfigFileContent}")
            this.script.echo("context.jenkinsFolderName: ${context.jenkinsFolderName}")
            this.script.echo("mothershipConfigFileContent repo: ${mothershipConfigFileContent[context.jenkinsFolderName]}")
            result = utils.merge(result, mothershipConfigFileContent[context.jenkinsFolderName])
            this.script.echo("mothershipConfigFileContent result: ${result}")
            this.configRepo = result.configRepo
        }
        result
    }

    def mergeScenariosConfigs(config, tempContext = [:], currentScenarioSourceName = null) {
        def scenariosConfig = [:]
        if (!tempContext) {
            tempContext << context
        }
        tempContext = utils.merge(tempContext, config)
        if (config.scenarios) {
            for (def i = 0; i < config.scenarios.size(); i++) {
                def s = config.scenarios[i]
                if (s instanceof String) {
                    def values = s.split(":")
                    def scenario = [:]
                    String scenarioSourceName
                    if (values.size() > 1) {
                        scenarioSourceName = values[0]
                        scenario.name = values[1]
                    }
                    else {
                        scenarioSourceName = currentScenarioSourceName
                        scenario.name = values[0]
                    }
                    utils.debugLog(context, tempContext.scenarioSources, 'Scenario sources')
                    if ((tempContext.scenarioSources && tempContext.scenarioSources.containsKey(scenarioSourceName)) || context.loadedSources.containsKey(scenarioSourceName)) {
                        if (!context.loadedSources[scenarioSourceName]) {
                            script.echo "Adding source: ${scenarioSourceName}"
                            scenario.source = tempContext.scenarioSources[scenarioSourceName]

                            script.sshagent([context.credentialsId]) {
                                def sourceObject = [
                                    name: scenarioSourceName,
                                    type: 'git',
                                    path: "scenarios/${scenarioSourceName}",
                                    url: scenario.source.repo,
                                    branch: scenario.source.ref ? scenario.source.ref : 'master',
                                    mode: 'shell',
                                ]

                                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], context)
                            }
                        }
                        else {
                            script.echo "Source: ${scenarioSourceName} already added"
                            scenario.source = context.loadedSources[scenarioSourceName]
                        }

                        def fileName = null

                        def sourceDir = utils.sourceDir(context, scenarioSourceName)

                        // Check config exists in sourceDir.
                        if (script.fileExists(sourceDir + "/scenarios/${scenario.name}/config.yaml")) {
                            fileName = sourceDir + "/scenarios/${scenario.name}/config.yaml"
                        }
                        else if (script.fileExists(sourceDir + "/scenarios/${scenario.name}/config.yml")) {
                            fileName = sourceDir + "/scenarios/${scenario.name}/config.yml"
                        }

                        // Merge scenario if exists.
                        if (fileName != null) {
                            script.echo "Scenario file name: ${fileName} exists"
                            def scenarioConfig = mergeScenariosConfigs(script.readYaml(file: fileName), tempContext, scenarioSourceName)
                            utils.debugLog(context, scenarioConfig, "Loaded scenario: ${scenarioSourceName}:${scenario.name} config")
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                            utils.debugLog(context, scenariosConfig, "Scenarios config")
                        }
                    }
                    else {
                        throw new RuntimeException("No scenario source with name: ${scenarioSourceName}")
                    }

                }
                else {
                    throw new RuntimeException("Not proper scenario config: ${s}")
                }
            }
        }
        utils.merge(scenariosConfig, config)
    }

    def projectConfig() {
        this.script.echo("projectConfig repo: ${context.configRepo}")
        if (context.configRepo) {
            def sourceObject = [
                name: 'project',
                path: 'sources/project',
                type: 'git',
                url: context.configRepo,
                branch: 'master',
                mode: 'shell',
            ]

            def providers = [
                [
                    action: 'Source.add',
                    params: [source: sourceObject]
                ],
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'project',
                        configType: 'groovy',
                        configPath: context.projectConfigFile
                    ]
                ],
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'project',
                        configType: 'yaml',
                        configPath: 'config.yaml'
                    ]
                ]
            ]
            def projectConfig
            script.sshagent([context.credentialsId]) {
                projectConfig = context.pipeline.executePipelineActionList(providers, context)
                utils.debugLog(context, projectConfig, 'Project config')
            }

            def result = mergeScenariosConfigs(projectConfig, [:], 'project')

            utils.debugLog(result, 'Project config with scenarios loaded')
            result
        }
    }

}
