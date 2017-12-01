package com.github.aroq.drupipe.providers.config

class ConfigProviderProject extends ConfigProviderBase {

    def provide() {
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

}
