package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Config extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def configRepo

    def perform() {
        if (action.pipeline.context['Config_perform']) {
            return
        }

        this.script.sh("mkdir -p .unipipe")
        this.script.sh("mkdir -p .unipipe/temp")

        def providers = [
            [
                action: 'GroovyFileConfig.groovyConfigFromLibraryResource',
                params: [
                    // We need to pass params as "context" is not ready yet.
                    store_result: true,
                    interpolate: false,
                    debugEnabled: true,
                    dump_result: true,
                    post_process: [
                        context: [
                            type: 'context',
                            source: '',
                            destination: '',
                        ],
                    ],
                    resource: 'com/github/aroq/drupipe/config.groovy'
                ]
            ],
            [
                action: "Config.envConfig",
//                params: [
//                    // We need to pass params as "context" is not ready yet.
//                    store_result: true,
//                    interpolate: false,
//                    debugEnabled: true,
//                    dump_result: true,
//                    post_process: [
//                        context: [
//                            type: 'context',
//                            source: '',
//                            destination: '',
//                        ],
//                    ],
//                ]
            ],
            [
                action: "Config.mothershipConfig",
//                params: [
//                    // We need to pass params as "context" is not ready yet.
//                    store_result: true,
//                    interpolate: false,
//                    debugEnabled: true,
//                    dump_result: true,
//                    post_process: [
//                        context: [
//                            type: 'context',
//                            source: '',
//                            destination: '',
//                        ],
//                    ],
//                ]
            ],
            [
                action: "Config.projectConfig"
            ],
            [
                action: "Config.jenkinsConfig"
            ],
            [
                action: "Config.jobConfig"
            ],
        ]

        if (action.pipeline.context.configProviders) {
            providers << action.pipeline.context.configProviders
        }

        this.script.checkout this.script.scm

        action.pipeline.executePipelineActionList(providers)

        // For compatibility:
        if (action.pipeline.context.defaultActionParams) {
            action.pipeline.context.params.action = utils.merge(action.pipeline.context.params.action, action.pipeline.context.defaultActionParams)
        }

        action.pipeline.context.environmentParams = [:]
        if (action.pipeline.context.environments) {
            if (action.pipeline.context.environment) {
                def environment = action.pipeline.context.environments[action.pipeline.context.environment]
                if (action.pipeline.context.servers && environment['server'] && action.pipeline.context.servers[environment['server']]) {
                    def server = action.pipeline.context.servers[environment['server']]
                    action.pipeline.context.environmentParams = utils.merge(server, environment)
                }
                else {
                    action.pipeline.context.environmentParams = environment
                }
                // For compatibility:
                if (action.pipeline.context.environmentParams) {
                    action.pipeline.context.params.action = utils.merge(context.params.action, context.environmentParams.defaultActionParams)
                }

                utils.jsonDump(action.pipeline.context, action.pipeline.context.environmentParams, 'ENVIRONMENT PARAMS')
            }
        }

        utils.debugLog(action.pipeline.context, action.pipeline.context, 'CONFIG CONTEXT')

//        context.return_stdout = false

        def stashes = action.pipeline.context.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')

        script.echo "Stashes: ${stashes}"

        script.stash name: 'config', includes: "${stashes}", excludes: ".git, .git/**"
       action.pipeline.context
    }

    def jenkinsConfig() {
        action.params.jenkinsParams
    }

    def jobConfig() {
        def result = [:]
        if (action.pipeline.context.jobs) {
            processJobs(action.pipeline.context.jobs)
            utils.jsonDump(action.pipeline.context, action.pipeline.context.jobs, 'CONFIG JOBS PROCESSED')

            result.job = (action.pipeline.context.env.JOB_NAME).split('/').drop(1).inject(action.pipeline.context, { obj, prop ->
                obj.jobs[prop]
            })

            if (result.job) {
                if (result.job.context) {
                    result = utils.merge(result, result.job.context)
                }
            }
            result.jobs = action.pipeline.context.jobs
        }
        result
    }

    def processJobs(jobs, prefixes = [], parentParams = [:]) {
        if (jobs) {
            for (job in jobs) {
                if (job.value.children) {
                    job.value.jobs = job.value.remove('children')
                }
                def children = job.value.jobs ? job.value.jobs : [:]
                job.value = utils.merge(parentParams, job.value)
                if (children) {
                    def jobClone = job.value.clone()
                    jobClone.remove('jobs')
                    processJobs(children, prefixes << job.key, jobClone)
                }
            }
        }
    }

    def envConfig() {
        def result = [:]
        result.workspace = this.script.pwd()
        result.env = this.utils.envToMap()
        result.credentialsId = result.env.credentialsId
//        result << result.env

        String jobPath = this.script.env.BUILD_URL ? this.script.env.BUILD_URL : this.script.env.JOB_DISPLAY_URL

        result.jenkinsFolderName = utils.getJenkinsFolderName(jobPath)
        result.jenkinsJobName = utils.getJenkinsJobName(jobPath)

        if (this.script.env.KUBERNETES_PORT) {
            result.containerMode = 'kubernetes'
        }
        utils.serializeAndDeserialize(result)
    }

    def mothershipConfig() {
        def result = [:]
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
                        credentialsId: action.pipeline.context.env.credentialsId,
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
            result = action.pipeline.executePipelineActionList(providers)
            def mothershipConfig = this.utils.getMothershipConfigFile(result)
            def mothershipServers = this.utils.getMothershipServersFile(result)

            result = utils.merge(result, mothershipConfig[action.pipeline.context.jenkinsFolderName])
            result = utils.merge(result, [jenkinsServers: mothershipServers])

//            this.configRepo = result.configRepo
        }
        result
    }

    def mergeScenariosConfigs(config, tempContext = [:], currentScenarioSourceName = null) {
        def scenariosConfig = [:]
        if (!tempContext) {
            tempContext << action.pipeline.context
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
                    utils.debugLog(action.pipeline.context, tempContext.scenarioSources, 'Scenario sources')
                    if ((tempContext.scenarioSources && tempContext.scenarioSources.containsKey(scenarioSourceName)) || action.pipeline.context.loadedSources.containsKey(scenarioSourceName)) {
                        if (!action.pipeline.context.loadedSources[scenarioSourceName]) {
                            script.echo "Adding source: ${scenarioSourceName}"
                            scenario.source = tempContext.scenarioSources[scenarioSourceName]

                            script.sshagent([action.pipeline.context.credentialsId]) {
                                def sourceObject = [
                                    name: scenarioSourceName,
                                    type: 'git',
                                    path: "scenarios/${scenarioSourceName}",
                                    url: scenario.source.repo,
                                    branch: scenario.source.ref ? scenario.source.ref : 'master',
                                    mode: 'shell',
                                ]

                                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], action.pipeline)
                            }
                        }
                        else {
                            utils.debugLog(action.pipeline.context, "Source: ${scenarioSourceName} already added")
                            scenario.source = action.pipeline.context.loadedSources[scenarioSourceName]
                        }

                        def fileName = null

                        def sourceDir = utils.sourceDir(action.pipeline.context, scenarioSourceName)

                        def filesToCheck = [
                            "/.unipipe/scenarios/${scenario.name}/config.yaml",
                            "/.unipipe/scenarios/${scenario.name}/config.yml",
                            "/.drupipe/scenarios/${scenario.name}/config.yaml",
                            "/.drupipe/scenarios/${scenario.name}/config.yml",
                            "/scenarios/${scenario.name}/config.yaml",
                            "/scenarios/${scenario.name}/config.yml"
                        ]

                        for (def ifc = 0; ifc < filesToCheck.size(); ifc++) {
                            def fileToCheck = filesToCheck[ifc]
                            if (script.fileExists(sourceDir + fileToCheck)) {
                                fileName = sourceDir + fileToCheck
                                break
                            }
                        }

                        // Merge scenario if exists.
                        if (fileName != null) {
                            utils.debugLog(action.pipeline.context, "Scenario file name: ${fileName} exists")
                            def scenarioConfig = mergeScenariosConfigs(script.readYaml(file: fileName), tempContext, scenarioSourceName)
                            utils.debugLog(action.pipeline.context, scenarioConfig, "Loaded scenario: ${scenarioSourceName}:${scenario.name} config")
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                            utils.debugLog(action.pipeline.context, scenariosConfig, "Scenarios config")
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
        utils.debugLog(action.pipeline.context, "projectConfig repo: ${action.pipeline.context.configRepo}", [:], [], true)
        if (action.pipeline.context.configRepo) {
            def sourceObject = [
                name: 'project',
                path: 'sources/project',
                type: 'git',
                url: action.pipeline.context.configRepo,
                branch: 'master',
                mode: 'shell',
            ]

            script.sshagent([action.pipeline.context.credentialsId]) {
                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], action.pipeline)
            }
//            utils.debugLog(action.pipeline.context, action.pipeline.context, "action.pipeline.context", [debugMode: 'json'], [], true)
//            utils.debugLog(action.pipeline.context, context, "context", [debugMode: 'json'], [], true)

            def providers = [
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'project',
                        configType: 'groovy',
                        configPath: action.pipeline.context.projectConfigFile
                    ]
                ],
            ]

            def fileName = null
            def sourceDir = utils.sourceDir(action.pipeline.context, 'project')
            this.script.echo("PROJECTS SOURCE DIR: ${sourceDir}")

            def filesToCheck = [
                ".unipipe/config.yaml",
                ".unipipe/config.yml",
                ".drupipe/config.yaml",
                ".drupipe/config.yml",
                "config.yaml",
                "config.yml"
            ]

            for (def ifc = 0; ifc < filesToCheck.size(); ifc++) {
                def fileToCheck = filesToCheck[ifc]
                def fileNameToCheck = sourceDir + '/' + fileToCheck
                this.script.echo("PROJECT FILE NAME TO CHECK: ${fileNameToCheck}")
                if (this.script.fileExists(fileNameToCheck)) {
                    this.script.echo("SELECTING PROJECT FILE: ${fileNameToCheck}")
                    fileName = fileToCheck
                    break
                }
            }

            if (fileName != null) {
              providers << [
                  action: 'Source.loadConfig',
                  params: [
                      sourceName: 'project',
                      configType: 'yaml',
                      configPath: fileName
                  ]
              ]
            }

            def projectConfig
            script.sshagent([action.pipeline.context.credentialsId]) {
                projectConfig = action.pipeline.executePipelineActionList(providers)
                utils.debugLog(action.pipeline.context, projectConfig, 'Project config')
            }

            def result = mergeScenariosConfigs(projectConfig, [:], 'project')

            utils.debugLog(result, 'Project config with scenarios loaded')
            result
        }
    }

}
