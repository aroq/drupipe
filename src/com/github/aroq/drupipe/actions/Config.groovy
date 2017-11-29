package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Config extends BaseAction {

    def script

    com.github.aroq.drupipe.Utils utils

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
                    post_process: [
                        context: [
                            type: 'result',
                            source: 'result',
                            destination: 'context',
                        ],
                    ],
                    resource: 'com/github/aroq/drupipe/config.groovy'
                ]
            ],
            [
                action: "Config.envConfig",
            ],
            [
                action: "Config.mothershipConfig",
            ],
            [
                action: "Config.config"
            ],
            [
                action: "Config.projectConfig"
            ],
//            [
//                action: "Config.config_version2"
//            ],
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
                if (action.pipeline.context.environmentParams && action.pipeline.context.environmentParams.defaultActionParams) {
                    action.pipeline.context.params.action = utils.merge(action.pipeline.context.params.action, action.pipeline.context.environmentParams.defaultActionParams)
                }

                utils.debugLog(action.pipeline.context, action.pipeline.context.environmentParams, 'ENVIRONMENT PARAMS', [:], [], true)
            }
            else {
                script.echo "No context.environment is defined"
            }
        }
        else {
            script.echo "No context.environments are defined"
        }

        utils.debugLog(action.pipeline.context, action.pipeline.context, 'CONFIG CONTEXT')

        def stashes = action.pipeline.context.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')

        script.echo "Stashes: ${stashes}"

        script.stash name: 'config', includes: "${stashes}", excludes: ".git, .git/**"
        [:]
    }

    def jenkinsConfig() {
        action.params.jenkinsParams
    }

    def jobConfig() {
        def result = [:]
        if (action.pipeline.context.jobs) {
            action.pipeline.archiveObjectJsonAndYaml(action.pipeline.context, 'context_unprocessed')

            // Performed here as needed later for job processing.
            action.pipeline.drupipeConfig.process()

            utils.log "AFTER jobConfig() action.pipeline.drupipeConfig.process()"

            utils.jsonDump(action.pipeline.context, action.pipeline.context.jobs, 'CONFIG JOBS PROCESSED - BEFORE processJobs', false)

            action.pipeline.context.jobs = processJobs(action.pipeline.context.jobs)

            utils.jsonDump(action.pipeline.context, action.pipeline.context.jobs, 'CONFIG JOBS PROCESSED - AFTER processJobs', false)

            result.job = (action.pipeline.context.env.JOB_NAME).split('/').drop(1).inject(action.pipeline.context, { obj, prop ->
                obj.jobs[prop]
            })

            if (result.job) {
                if (result.job.context) {
                    result = utils.merge(result, result.job.context)
                }
            }
        }
        else {
            utils.log "Config.jobConfig() -> No action.pipeline.context.jobs are defined"
        }
        result
    }

    def processJobs(jobs, parentParams = [:]) {
        def result = jobs
        for (job in jobs) {
            // For compatibility with previous config versions.
            if (job.value.children) {
                job.value.jobs = job.value.remove('children')
            }
            if (job.value.jobs) {
                def params = job.value.clone()
                params.remove('jobs')
                job.value.jobs = processJobs(job.value.jobs, params)
            }
            if (parentParams) {
                result[job.key] = utils.merge(parentParams, job.value)
            }
            else {
                result[job.key] = job.value
            }
        }
        result
    }

    def envConfig() {
        def result = [:]
        result.workspace = this.script.pwd()
        result.env = this.utils.envToMap()
        // TODO: Use env vars pattern to override.
        result.credentialsId = result.env.credentialsId
        result.environment = result.env.environment
        result.configRepo = result.env.configRepo

        String jobPath = script.env.BUILD_URL ? script.env.BUILD_URL : script.env.JOB_DISPLAY_URL
        result.jenkinsFolderName = utils.getJenkinsFolderName(jobPath)
        result.jenkinsJobName = utils.getJenkinsJobName(jobPath)

        if (script.env.KUBERNETES_PORT) {
            result.containerMode = 'kubernetes'
        }
        utils.serializeAndDeserialize(result)
    }

    def mothershipConfig() {
        def result = [:]
        if (action.pipeline.context.env.MOTHERSHIP_REPO) {
            def sourceObject = [
                name:   'mothership',
                type:   'git',
                path:   '.unipipe/mothership',
                url:    script.env.MOTHERSHIP_REPO,
                branch: 'master',
            ]

            this.script.drupipeAction([action: "Source.add", params: [credentialsId: action.pipeline.context.env.credentialsId, source: sourceObject]], action.pipeline)

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
            utils.debugLog(action.pipeline.context, mothershipConfig, 'mothershipConfig', [debugMode: 'json'], [], false)
            def mothershipServers = this.utils.getMothershipServersFile(result)
            utils.debugLog(action.pipeline.context, mothershipServers, 'mothershipServers', [debugMode: 'json'], [], false)

            def mothershipProjectConfig = mothershipConfig[action.pipeline.context.jenkinsFolderName]
            script.echo "mothershipProjectConfig: ${mothershipProjectConfig}"

            result = utils.merge(result, mothershipProjectConfig)
            utils.debugLog(action.pipeline.context, result, 'mothershipServer result after merge', [debugMode: 'json'], [], false)
            result = utils.merge(result, [jenkinsServers: mothershipServers])
            utils.debugLog(action.pipeline.context, result, 'mothershipServer result2 after merge', [debugMode: 'json'], [], false)
        }
        result
    }

    def mergeScenariosConfigs(context, config, tempContext = [:], currentScenarioSourceName = null) {
        def uniconfIncludeKey = utils.deepGet(context, 'uniconf.keys.include')
        def uniconfSourcesKey = utils.deepGet(context, 'uniconf.keys.sources')

//        utils.log "uniconfIncludeKey: ${uniconfIncludeKey}"
//        utils.log "uniconfSourcesKey: ${uniconfSourcesKey}"

        if (config.containsKey('scenarios') && uniconfIncludeKey != 'scenarios') {
            config[uniconfIncludeKey] = config['scenarios']
        }

        def scenariosConfig = [:]
        if (!tempContext) {
            tempContext << action.pipeline.context
        }

        tempContext = utils.merge(tempContext, config)
        if (config.containsKey(uniconfIncludeKey)) {
//            utils.log "config.containsKey(uniconfIncludeKey)"
            // Iterate through 'include' keys.
            for (def i = 0; i < config[uniconfIncludeKey].size(); i++) {
                def s = config[uniconfIncludeKey][i]
                if (s instanceof String) {
                    // Process include key value to extract source and file names.
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
//                    utils.log("scenarioSourceName: ${scenarioSourceName}")
//                    utils.log("scenario.name: ${scenario.name}")

                    utils.debugLog(action.pipeline.context, tempContext[uniconfSourcesKey], 'Scenario sources', ['debugMode': 'json'], [], false)

                    if (
                        (scenariosConfig[uniconfSourcesKey] && scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || (tempContext[uniconfSourcesKey] && tempContext[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || action.pipeline.context.loadedSources.containsKey(scenarioSourceName)
                    )
                    {
                        if (!action.pipeline.context.loadedSources[scenarioSourceName]) {
                            utils.log "Adding source: ${scenarioSourceName}"
                            if (tempContext[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = tempContext[uniconfSourcesKey][scenarioSourceName]
                            }
                            else if (scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = scenariosConfig[uniconfSourcesKey][scenarioSourceName]
                            }

                            script.sshagent([action.pipeline.context.credentialsId]) {
                                def sourceObject = [
                                    name: scenarioSourceName,
                                    type: 'git',
                                    path: ".unipipe/scenarios/${scenarioSourceName}",
                                    url: scenario.source.repo,
                                    branch: scenario.source.ref ? scenario.source.ref : 'master',
                                    mode: 'shell',
                                ]

                                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], action.pipeline)
//                                utils.log "Source added: ${scenarioSourceName}"
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
                            def scenarioConfig = mergeScenariosConfigs(context, script.readYaml(file: fileName), tempContext, scenarioSourceName)
                            utils.debugLog(action.pipeline.context, scenarioConfig, "Loaded scenario: ${scenarioSourceName}:${scenario.name} config")
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                            utils.debugLog(action.pipeline.context, scenariosConfig, "Scenarios config")
                        }
                        else {
                            utils.log "Scenario file doesn't found"
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
        utils.debugLog(action.pipeline.context, action.pipeline.context.configRepo,"projectConfig repo: ${action.pipeline.context.configRepo}", [:], [], true)
        if (action.pipeline.context.project_type == 'single') {
            def sourceObject = [
                name  : 'project',
                path  : action.pipeline.context.config_dir,
                type  : 'dir',
            ]
            this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], action.pipeline)
        }
        else {
            if (action.pipeline.context.containsKey('job') && action.pipeline.context.job.containsKey('configRepo') && action.pipeline.context.job.configRepo) {
                def sourceObject = [
                    name  : 'project',
                    path  : action.pipeline.context.projectConfig,
                    type  : 'git',
                    url   : action.pipeline.context.job.configRepo,
                    branch: 'master',
                    mode  : 'shell',
                ]
                script.sshagent([action.pipeline.context.credentialsId]) {
                    this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], action.pipeline)
                }
            }
            else {
                def sourceObject = [
                    name  : 'project',
                    path  : action.pipeline.context.projectConfigPath,
                    type  : 'dir',
                ]
                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], action.pipeline)
            }
        }
        if (action.pipeline.context.configRepo) {
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

                if (projectConfig.config_version > 1 || action.pipeline.configVersion() > 1) {
                    projectConfig = utils.merge(config_version2(), projectConfig)
                }
            }

            def projectConfigContext = utils.merge(action.pipeline.context, projectConfig)

            def sources = [:]
            if (action.pipeline.context.env.containsKey('UNIPIPE_SOURCES')) {
                utils.log "Processing UNIPIPE_SOURCES"
                def uniconfSourcesKey = utils.deepGet(projectConfigContext, 'uniconf.keys.sources')
                sources[uniconfSourcesKey] = script.readJSON(text: action.pipeline.context.env['UNIPIPE_SOURCES'])
                if (projectConfig[uniconfSourcesKey]) {
                    projectConfig[uniconfSourcesKey] << sources[uniconfSourcesKey]
                }
                else {
                    projectConfig[uniconfSourcesKey] = sources[uniconfSourcesKey]
                }

                utils.debugLog(projectConfig, sources, 'UNIPIPE_SOURCES sources', ['debugMode': 'json'], [], true)
            }

            def result = mergeScenariosConfigs(projectConfigContext, projectConfig, [:], 'project')

            utils.debugLog(result, 'Project config with scenarios loaded')
            result
        }
    }

    def config() {
        def providers = [
            [
                action: 'YamlFileConfig.loadFromLibraryResource',
                params: [
                    resource: 'com/github/aroq/drupipe/config.yaml'
                ]
            ],
        ]

        action.pipeline.executePipelineActionList(providers)
    }

    def config_version2() {
//        if (action.pipeline.configVersion() > 1) {
            def providers = [
                [
                    action: 'YamlFileConfig.loadFromLibraryResource',
                    params: [
                        resource: 'com/github/aroq/drupipe/config_version2.yaml'
                    ]
                ],
                [
                    action: 'YamlFileConfig.loadFromLibraryResource',
                    params: [
                        resource: 'com/github/aroq/drupipe/actions.yaml'
                    ]
                ],
            ]

            action.pipeline.executePipelineActionList(providers)
//        }
//        else {
//            [:]
//        }
    }

}
