package com.github.aroq.drupipe

import com.github.aroq.drupipe.processors.DrupipeProcessor
import com.github.aroq.drupipe.processors.DrupipeProcessorsController
import com.github.aroq.drupipe.providers.config.ConfigProvider

class DrupipeConfig implements Serializable {

    def controller

    def script

    com.github.aroq.drupipe.Utils utils

    def config

    ArrayList<ConfigProvider> configProviders = []

    @NonCPS
    def groovyConfig(text) {
        new HashMap<>(ConfigSlurper.newInstance(script.env.drupipeEnvironment).parse(text))
    }

    def config(params, parent) {
        script.node('master') {
//            utils.log "Executing pipeline"

            params.debugEnabled = params.debugEnabled && params.debugEnabled != '0' ? true : false
            utils.dump(params, params, 'PIPELINE-PARAMS')
            // Get config (context).
//            script.drupipeAction([action: 'Config.perform', params: [jenkinsParams: params]], controller)

            config = groovyConfig(script.libraryResource('com/github/aroq/drupipe/config.groovy'))
            utils.serializeAndDeserialize(config)

            // TODO: Perform SCM checkout only when really needed.
            this.script.checkout this.script.scm

            for (def i = 0; i < config.config_providers_list.size(); i++) {
                def properties = [script: script, utils: utils, config: this, controller: controller]
                def className = "com.github.aroq.drupipe.providers.config.${config.config_providers[config.config_providers_list[i]].class_name}"
                script.echo "Config Provider class name: ${className}"
                configProviders.add(parent.class.classLoader.loadClass(className, true, false)?.newInstance(
                    properties
                ))
            }

            for (def i = 0; i < configProviders.size(); i++) {
                ConfigProvider configProvider = configProviders[i]
                config = utils.merge(config, configProvider.provide())
            }

//            config = utils.merge(config, envConfig())
//            config = utils.merge(config, mothershipConfig())
//            config = utils.merge(config, configMain())
//            config = utils.merge(config, projectConfig())
//            config = utils.merge(config, jenkinsConfig())
//            config = utils.merge(config, jobConfig())
            

//            controller.executePipelineActionList(providers)

            // For compatibility:
            if (config.defaultActionParams) {
                config.params.action = utils.merge(config.params.action, config.defaultActionParams)
            }

            config.environmentParams = [:]
            if (config.environments) {
                if (config.environment) {
                    def environment = config.environments[config.environment]
                    if (config.servers && environment['server'] && config.servers[environment['server']]) {
                        def server = config.servers[environment['server']]
                        config.environmentParams = utils.merge(server, environment)
                    }
                    else {
                        config.environmentParams = environment
                    }
                    // For compatibility:
                    if (config.environmentParams && config.environmentParams.defaultActionParams) {
                        config.params.action = utils.merge(config.params.action, config.environmentParams.defaultActionParams)
                    }

                    utils.debugLog(config, config.environmentParams, 'ENVIRONMENT PARAMS', [:], [], true)
                }
                else {
                    script.echo "No context.environment is defined"
                }
            }
            else {
                script.echo "No context.environments are defined"
            }

            utils.debugLog(config, config, 'CONFIG CONTEXT')

            def stashes = config.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')

            script.echo "Stashes: ${stashes}"

            script.stash name: 'config', includes: "${stashes}", excludes: ".git, .git/**"

            controller.archiveObjectJsonAndYaml(config, 'context')
        }
        return config
    }

    int configVersion() {
        config.config_version as int
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
        controller.drupipeProcessorsController.process(config, item, parentKey, paramsKey, mode)
    }

    def process() {
        utils.log "DrupipeConfig->process()"
        if (controller.configVersion() > 1) {
//            controller.drupipeProcessorsController = initProcessorsController(this, config.processors)
            config.jobs = processItem(config.jobs, 'context', 'params', 'config')
            controller.archiveObjectJsonAndYaml(config, 'context_processed')
        }
    }

    def jenkinsConfig() {
        action.params.jenkinsParams
    }

    def jobConfig() {
        def result = [:]
        if (config.jobs) {
            controller.archiveObjectJsonAndYaml(config, 'context_unprocessed')

            // Performed here as needed later for job processing.
            controller.drupipeConfig.process()

            utils.log "AFTER jobConfig() controller.drupipeConfig.process()"

            utils.jsonDump(config, config.jobs, 'CONFIG JOBS PROCESSED - BEFORE processJobs', false)

            config.jobs = processJobs(config.jobs)

            utils.jsonDump(config, config.jobs, 'CONFIG JOBS PROCESSED - AFTER processJobs', false)

            result.job = (config.env.JOB_NAME).split('/').drop(1).inject(config, { obj, prop ->
                obj.jobs[prop]
            })

            if (result.job) {
                if (result.job.context) {
                    result = utils.merge(result, result.job.context)
                }
            }
        }
        else {
            utils.log "Config.jobConfig() -> No config.jobs are defined"
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

    def mothershipConfig() {
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
            tempContext << config
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

                    utils.debugLog(config, tempContext[uniconfSourcesKey], 'Scenario sources', ['debugMode': 'json'], [], false)

                    if (
                    (scenariosConfig[uniconfSourcesKey] && scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || (tempContext[uniconfSourcesKey] && tempContext[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || config.loadedSources.containsKey(scenarioSourceName)
                    )
                    {
                        if (!config.loadedSources[scenarioSourceName]) {
                            utils.log "Adding source: ${scenarioSourceName}"
                            if (tempContext[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = tempContext[uniconfSourcesKey][scenarioSourceName]
                            }
                            else if (scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = scenariosConfig[uniconfSourcesKey][scenarioSourceName]
                            }

                            script.sshagent([config.credentialsId]) {
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
                            utils.debugLog(config, "Source: ${scenarioSourceName} already added")
                            scenario.source = config.loadedSources[scenarioSourceName]
                        }

                        def fileName = null

                        def sourceDir = utils.sourceDir(config, scenarioSourceName)

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
                            utils.debugLog(config, "Scenario file name: ${fileName} exists")
                            def scenarioConfig = mergeScenariosConfigs(context, script.readYaml(file: fileName), tempContext, scenarioSourceName)
                            utils.debugLog(config, scenarioConfig, "Loaded scenario: ${scenarioSourceName}:${scenario.name} config")
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                            utils.debugLog(config, scenariosConfig, "Scenarios config")
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
        utils.debugLog(config, config.configRepo,"projectConfig repo: ${config.configRepo}", [:], [], true)
        if (config.project_type == 'single') {
            def sourceObject = [
                name  : 'project',
                path  : config.config_dir,
                type  : 'dir',
            ]
            this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], controller)
        }
        else {
            if (config.configRepo) {
                def sourceObject = [
                    name  : 'project',
                    path  : config.projectConfigPath,
                    type  : 'git',
                    url   : config.configRepo,
                    branch: 'master',
                    mode  : 'shell',
                ]
                script.sshagent([config.credentialsId]) {
                    this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], controller)
                }
            }
        }
        if (config.configRepo) {
//            utils.debugLog(config, config, "config", [debugMode: 'json'], [], true)
//            utils.debugLog(config, context, "context", [debugMode: 'json'], [], true)

            def providers = [
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'project',
                        configType: 'groovy',
                        configPath: config.projectConfigFile
                    ]
                ],
            ]

            def fileName = null
            def sourceDir = utils.sourceDir(config, 'project')
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
            script.sshagent([config.credentialsId]) {
                projectConfig = controller.executePipelineActionList(providers)
                utils.debugLog(config, projectConfig, 'Project config')

                if (projectConfig.config_version > 1 || controller.configVersion() > 1) {
                    projectConfig = utils.merge(config_version2(), projectConfig)
                }
            }

            def projectConfigContext = utils.merge(config, projectConfig)

            def sources = [:]
            if (config.env.containsKey('UNIPIPE_SOURCES')) {
                utils.log "Processing UNIPIPE_SOURCES"
                def uniconfSourcesKey = utils.deepGet(projectConfigContext, 'uniconf.keys.sources')
                sources[uniconfSourcesKey] = script.readJSON(text: config.env['UNIPIPE_SOURCES'])
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
