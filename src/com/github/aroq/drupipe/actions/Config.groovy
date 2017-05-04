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

    def mergeScenariosConfigs(config) {
        def scenariosConfig = [:]
        if (config.scenarios) {
            for (def i = 0; i < config.scenarios.size(); i++) {
                def s = config.scenarios[i]
                if (s instanceof String) {
                    def values = s.split(":")
                    def scenario = [:]
                    String scenarioSource
                    if (values.size() > 1) {
                        scenarioSource = values[0]
                        scenario.name = values[1]
                    }
                    else {
                        scenarioSource = config.default_scenario_source
                        scenario.name = values[0]
                    }
                    script.echo "Scenario source: ${scenarioSource}"
                    if (config.scenario_sources[scenarioSource]) {
                        scenario.source = config.scenario_sources[scenarioSource]
                        if (!this.scenarioSources[scenario.source]) {
                            scenario.source.repoParams = [
                                repoAddress: scenario.source.repo,
                                reference: scenario.source.ref ? scenario.source.ref : 'master',
                                dir: 'scenarios',
                                repoDirName: scenarioSource,
                            ]
                            script.sshagent([context.credentialsId]) {
                                this.script.drupipeAction([action: "Git.clone", params: scenario.source.repoParams], context)
                            }
                        }
                        def sourceDir = scenario.source.repoParams.dir + '/' + scenario.source.repoParams.repoDirName
                        def fileName = "${sourceDir}/scenarios/${scenario.name}/config.yaml"
                        script.echo "Scenario file name: ${fileName}"
                        if (script.fileExists(fileName)) {
                            script.echo "Scenario file name: ${fileName} exists"
                            def scenarioConfig = mergeScenariosConfigs(script.readYaml(file: fileName))
                            utils.dump(scenarioConfig)
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                        }
                    }
                    else {
                        throw "No scenario source with name: ${scenarioSource}"
                    }

                }
                else {
                    throw "Not proper scenario config: ${s}"
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
        def projectConfig = context.pipeline.executePipelineActionList(providers, context)
        script.echo "Project config"
        utils.dump(projectConfig)

//        def sourceDir = utils.sourceDir(context, 'mothershipConfig')

        mergeScenariosConfigs(projectConfig)
    }

}
