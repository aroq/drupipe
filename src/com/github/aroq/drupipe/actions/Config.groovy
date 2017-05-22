package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Config extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    LinkedHashMap scenarioSources = [:]

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
        if (context.environments && context.servers) {
            if (context.environment) {
                def environment = context.environments[context.environment]
                def server = context.servers[environment['server']]
                context.environmentParams = utils.merge(server, environment)
                context.defaultActionParams = utils.merge(context.defaultActionParams, context.environmentParams.defaultActionParams)
                utils.jsonDump(context.environmentParams, 'ENVIRONMENT PARAMS')
            }
        }
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
        result.jenkinsFolderName = this.utils.getJenkinsFolderName(this.script.env.BUILD_URL)
        result.jenkinsJobName = this.utils.getJenkinsJobName(this.script.env.BUILD_URL)
        result
    }

    def mothershipConfig() {
        def result
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
            result = context.pipeline.executePipelineActionList(providers, context)
            def json = this.script.readFile('mothership/projects.json')
            result = utils.merge(result, this.utils.getMothershipProjectParams(context, json))

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
                    //utils.dump(tempContext.scenarioSources, 'Scenario sources')
                    if (tempContext.scenarioSources[scenarioSourceName]) {
                        if (!this.scenarioSources[scenarioSourceName]) {
                            scenario.source = tempContext.scenarioSources[scenarioSourceName]
                            scenario.source.repoParams = [
                                repoAddress: scenario.source.repo,
                                reference: scenario.source.ref ? scenario.source.ref : 'master',
                                dir: 'scenarios',
                                repoDirName: scenarioSourceName,
                            ]
                            script.sshagent([context.credentialsId]) {
                                this.script.drupipeAction([action: "Git.clone", params: scenario.source.repoParams], context)

                                def sourceObject = [
                                    name: scenarioSourceName,
                                    type: 'dir',
                                    path: "${scenario.source.repoParams.dir}/${scenario.source.repoParams.repoDirName}",
                                ]

                                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], context)
//                                this.script.drupipeAction([action: "Source.loadConfig", params: [
//                                    sourceName: scenarioSourceName,
//                                    configType: 'yaml',
//                                    configPath: 'config.yaml',
//                                ]])
                            }
                            this.scenarioSources[scenarioSourceName] = scenario.source
                        }
                        else {
                            scenario.source = this.scenarioSources[scenarioSourceName]
                        }
                        def sourceDir = scenario.source.repoParams.dir + '/' + scenario.source.repoParams.repoDirName
                        def fileName = "${sourceDir}/scenarios/${scenario.name}/config.yaml"
                        if (script.fileExists(fileName)) {
                            script.echo "Scenario file name: ${fileName} exists"
                            def scenarioConfig = mergeScenariosConfigs(script.readYaml(file: fileName), tempContext, scenarioSourceName)
                            utils.dump(scenarioConfig, "Loaded scenario: ${scenarioSourceName}:${scenario.name} config")
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                            utils.dump(scenariosConfig, "Scenarios config")
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
        def sourceObject = [
            name: 'projectConfig',
            type: 'dir',
            path: context.projectConfigPath,
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
                    configPath: context.projectConfigFile
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
        def projectConfig = context.pipeline.executePipelineActionList(providers, context)
        //utils.jsonDump(projectConfig, 'Project config')

        if (!projectConfig.scenarioSources) {
            projectConfig.scenarioSources = [:]
        }
        projectConfig.scenarioSources << [
            mothership: [
                repo: this.script.env.MOTHERSHIP_REPO
            ]
        ]
        def rootConfigSource = [
            root_config: [
                repoParams: [
                    dir: 'docroot',
                    repoDirName: 'config',
                ]
            ]
        ]

        projectConfig.scenarioSources << rootConfigSource

        this.scenarioSources = [:]
        this.scenarioSources << rootConfigSource

        def result = mergeScenariosConfigs(projectConfig, [:], 'root_config')

        utils.jsonDump(this.scenarioSources.keySet() as List, "Scenarios loaded")
        utils.dump(result, 'Project config with scenarios loaded')
        result
    }

}
