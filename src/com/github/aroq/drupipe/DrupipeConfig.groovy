package com.github.aroq.drupipe

import com.github.aroq.drupipe.processors.DrupipeProcessor
import com.github.aroq.drupipe.processors.DrupipeProcessorsController

class DrupipeConfig implements Serializable {

    def controller

    def script

    def utils

    @NonCPS
    def groovyConfig(text) {
        new HashMap<>(ConfigSlurper.newInstance(script.env.drupipeEnvironment).parse(text))
    }

    def config(params) {
        def config
        script.node('master') {
//            utils.log "Executing pipeline"
            params.debugEnabled = params.debugEnabled && params.debugEnabled != '0' ? true : false
            utils.dump(params, params, 'PIPELINE-PARAMS')
            // Get config (context).
//            script.drupipeAction([action: 'Config.perform', params: [jenkinsParams: params]], controller)

            config = groovyConfig(script.libraryResource('com/github/aroq/drupipe/config.groovy'))
            utils.serializeAndDeserialize(config)

            this.script.checkout this.script.scm

            config = utils.merge(config, envConfig())
            config = utils.merge(config, mothershipConfig())
            config = utils.merge(config, configMain())
            config = utils.merge(config, projectConfig())
            config = utils.merge(config, jenkinsConfig())
            config = utils.merge(config, jobConfig())
            
            // TODO: Perform SCM checkout only when really needed.

//            controller.executePipelineActionList(providers)

            // For compatibility:
            if (controller.context.defaultActionParams) {
                controller.context.params.action = utils.merge(controller.context.params.action, controller.context.defaultActionParams)
            }

            controller.context.environmentParams = [:]
            if (controller.context.environments) {
                if (controller.context.environment) {
                    def environment = controller.context.environments[controller.context.environment]
                    if (controller.context.servers && environment['server'] && controller.context.servers[environment['server']]) {
                        def server = controller.context.servers[environment['server']]
                        controller.context.environmentParams = utils.merge(server, environment)
                    }
                    else {
                        controller.context.environmentParams = environment
                    }
                    // For compatibility:
                    if (controller.context.environmentParams && controller.context.environmentParams.defaultActionParams) {
                        controller.context.params.action = utils.merge(controller.context.params.action, controller.context.environmentParams.defaultActionParams)
                    }

                    utils.debugLog(controller.context, controller.context.environmentParams, 'ENVIRONMENT PARAMS', [:], [], true)
                }
                else {
                    script.echo "No context.environment is defined"
                }
            }
            else {
                script.echo "No context.environments are defined"
            }

            utils.debugLog(controller.context, controller.context, 'CONFIG CONTEXT')

            def stashes = controller.context.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')

            script.echo "Stashes: ${stashes}"

            script.stash name: 'config', includes: "${stashes}", excludes: ".git, .git/**"

            controller.archiveObjectJsonAndYaml(controller.context, 'context')
        }
        return config
    }

    int configVersion() {
        controller.context.config_version as int
    }

    DrupipeProcessorsController initProcessorsController(parent, processorsConfig) {
        utils.log "initProcessorsController"
        ArrayList<DrupipeProcessor> processors = []
        for (processorConfig in processorsConfig) {
            utils.log "Processor: ${processorConfig.className}"
            try {
                def properties = [utils: utils]
                if (processorConfig.properties) {
                    properties << processorConfig.properties
                }
                processors << parent.class.classLoader.loadClass("com.github.aroq.drupipe.processors.${processorConfig.className}", true, false)?.newInstance(
                    properties
                )
                utils.log "Processor: ${processorConfig.className} created"
            }
            catch (err) {
                throw err
            }
        }
        new DrupipeProcessorsController(processors: processors, utils: utils)
    }

    def processItem(item, parentKey, paramsKey = 'params', mode) {
        utils.log "DrupipeConfig->processItem"
        controller.drupipeProcessorsController.process(controller.context, item, parentKey, paramsKey, mode)
    }

    def process() {
        utils.log "DrupipeConfig->process()"
        if (controller.configVersion() > 1) {
//            controller.drupipeProcessorsController = initProcessorsController(this, controller.context.processors)
            controller.context.jobs = processItem(controller.context.jobs, 'context', 'params', 'config')
            controller.archiveObjectJsonAndYaml(controller.context, 'context_processed')
        }
    }

    def jenkinsConfig() {
        action.params.jenkinsParams
    }

    def jobConfig() {
        def result = [:]
        if (controller.context.jobs) {
            controller.archiveObjectJsonAndYaml(controller.context, 'context_unprocessed')

            // Performed here as needed later for job processing.
            controller.drupipeConfig.process()

            utils.log "AFTER jobConfig() controller.drupipeConfig.process()"

            utils.jsonDump(controller.context, controller.context.jobs, 'CONFIG JOBS PROCESSED - BEFORE processJobs', false)

            controller.context.jobs = processJobs(controller.context.jobs)

            utils.jsonDump(controller.context, controller.context.jobs, 'CONFIG JOBS PROCESSED - AFTER processJobs', false)

            result.job = (controller.context.env.JOB_NAME).split('/').drop(1).inject(controller.context, { obj, prop ->
                obj.jobs[prop]
            })

            if (result.job) {
                if (result.job.context) {
                    result = utils.merge(result, result.job.context)
                }
            }
        }
        else {
            utils.log "Config.jobConfig() -> No controller.context.jobs are defined"
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
        if (controller.context.env.MOTHERSHIP_REPO) {
            def sourceObject = [
                name:   'mothership',
                type:   'git',
                path:   '.unipipe/mothership',
                url:    script.env.MOTHERSHIP_REPO,
                branch: 'master',
            ]

            this.script.drupipeAction([action: "Source.add", params: [credentialsId: controller.context.env.credentialsId, source: sourceObject]], controller)

            def providers = [
                [
                    action: 'Source.add',
                    params: [
                        source: sourceObject,
                        credentialsId: controller.context.env.credentialsId,
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
            result = controller.executePipelineActionList(providers)
            def mothershipConfig = this.utils.getMothershipConfigFile(result)
            utils.debugLog(controller.context, mothershipConfig, 'mothershipConfig', [debugMode: 'json'], [], false)
            def mothershipServers = this.utils.getMothershipServersFile(result)
            utils.debugLog(controller.context, mothershipServers, 'mothershipServers', [debugMode: 'json'], [], false)

            def mothershipProjectConfig = mothershipConfig[controller.context.jenkinsFolderName]
            script.echo "mothershipProjectConfig: ${mothershipProjectConfig}"

            result = utils.merge(result, mothershipProjectConfig)
            utils.debugLog(controller.context, result, 'mothershipServer result after merge', [debugMode: 'json'], [], false)
            result = utils.merge(result, [jenkinsServers: mothershipServers])
            utils.debugLog(controller.context, result, 'mothershipServer result2 after merge', [debugMode: 'json'], [], false)

            if (result.config_version > 1) {
                utils.log "Initialising drupipeProcessorsController"
                controller.drupipeProcessorsController = controller.drupipeConfig.initProcessorsController(this, controller.context.processors)
            }
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
            tempContext << controller.context
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

                    utils.debugLog(controller.context, tempContext[uniconfSourcesKey], 'Scenario sources', ['debugMode': 'json'], [], false)

                    if (
                    (scenariosConfig[uniconfSourcesKey] && scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || (tempContext[uniconfSourcesKey] && tempContext[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || controller.context.loadedSources.containsKey(scenarioSourceName)
                    )
                    {
                        if (!controller.context.loadedSources[scenarioSourceName]) {
                            utils.log "Adding source: ${scenarioSourceName}"
                            if (tempContext[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = tempContext[uniconfSourcesKey][scenarioSourceName]
                            }
                            else if (scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = scenariosConfig[uniconfSourcesKey][scenarioSourceName]
                            }

                            script.sshagent([controller.context.credentialsId]) {
                                def sourceObject = [
                                    name: scenarioSourceName,
                                    type: 'git',
                                    path: ".unipipe/scenarios/${scenarioSourceName}",
                                    url: scenario.source.repo,
                                    branch: scenario.source.ref ? scenario.source.ref : 'master',
                                    mode: 'shell',
                                ]

                                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], controller)
//                                utils.log "Source added: ${scenarioSourceName}"
                            }
                        }
                        else {
                            utils.debugLog(controller.context, "Source: ${scenarioSourceName} already added")
                            scenario.source = controller.context.loadedSources[scenarioSourceName]
                        }

                        def fileName = null

                        def sourceDir = utils.sourceDir(controller.context, scenarioSourceName)

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
                            utils.debugLog(controller.context, "Scenario file name: ${fileName} exists")
                            def scenarioConfig = mergeScenariosConfigs(context, script.readYaml(file: fileName), tempContext, scenarioSourceName)
                            utils.debugLog(controller.context, scenarioConfig, "Loaded scenario: ${scenarioSourceName}:${scenario.name} config")
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                            utils.debugLog(controller.context, scenariosConfig, "Scenarios config")
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
        utils.debugLog(controller.context, controller.context.configRepo,"projectConfig repo: ${controller.context.configRepo}", [:], [], true)
        if (controller.context.project_type == 'single') {
            def sourceObject = [
                name  : 'project',
                path  : controller.context.config_dir,
                type  : 'dir',
            ]
            this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], controller)
        }
        else {
            if (controller.context.configRepo) {
                def sourceObject = [
                    name  : 'project',
                    path  : controller.context.projectConfigPath,
                    type  : 'git',
                    url   : controller.context.configRepo,
                    branch: 'master',
                    mode  : 'shell',
                ]
                script.sshagent([controller.context.credentialsId]) {
                    this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], controller)
                }
            }
        }
        if (controller.context.configRepo) {
//            utils.debugLog(controller.context, controller.context, "controller.context", [debugMode: 'json'], [], true)
//            utils.debugLog(controller.context, context, "context", [debugMode: 'json'], [], true)

            def providers = [
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'project',
                        configType: 'groovy',
                        configPath: controller.context.projectConfigFile
                    ]
                ],
            ]

            def fileName = null
            def sourceDir = utils.sourceDir(controller.context, 'project')
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
            script.sshagent([controller.context.credentialsId]) {
                projectConfig = controller.executePipelineActionList(providers)
                utils.debugLog(controller.context, projectConfig, 'Project config')

                if (projectConfig.config_version > 1 || controller.configVersion() > 1) {
                    projectConfig = utils.merge(config_version2(), projectConfig)
                }
            }

            def projectConfigContext = utils.merge(controller.context, projectConfig)

            def sources = [:]
            if (controller.context.env.containsKey('UNIPIPE_SOURCES')) {
                utils.log "Processing UNIPIPE_SOURCES"
                def uniconfSourcesKey = utils.deepGet(projectConfigContext, 'uniconf.keys.sources')
                sources[uniconfSourcesKey] = script.readJSON(text: controller.context.env['UNIPIPE_SOURCES'])
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

    def configMain() {
        def providers = [
            [
                action: 'YamlFileConfig.loadFromLibraryResource',
                params: [
                    resource: 'com/github/aroq/drupipe/config.yaml'
                ]
            ],
        ]

        controller.executePipelineActionList(providers)
    }

    def config_version2() {
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

        def result = controller.executePipelineActionList(providers)

        result
    }

}
