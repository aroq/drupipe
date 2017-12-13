package com.github.aroq.drupipe.providers.config

class ConfigProviderProject extends ConfigProviderBase {

    def provide() {
        def projectConfig
        controller.drupipeLogger.debugLog(drupipeConfig.config, drupipeConfig.config.configRepo,"projectConfig repo: ${drupipeConfig.config.configRepo}", [:])

        if (drupipeConfig.config.project_type == 'single') {
            def source= [
                name  : 'project',
                path  : drupipeConfig.config.config_dir,
                type  : 'dir',
            ]
            drupipeConfig.drupipeSourcesController.sourceAdd(source)
        }
        else {
            if (drupipeConfig.config.configRepo) {
                def source= [
                    name  : 'project',
                    path  : drupipeConfig.config.projectConfigPath,
                    type  : 'git',
                    url   : drupipeConfig.config.configRepo,
                    branch: 'master',
                    mode  : 'shell',
                ]
                script.sshagent([drupipeConfig.config.credentialsId]) {
                    drupipeConfig.drupipeSourcesController.sourceAdd(source)
                }
            }
        }
        if (drupipeConfig.config.configRepo) {
            projectConfig = drupipeConfig.drupipeSourcesController.sourceLoad(
                sourceName: 'project',
                configType: 'groovy',
                configPath: drupipeConfig.config.projectConfigFile,
            )

            def fileName = null
            controller.drupipeLogger.debugLog(drupipeConfig.config, drupipeConfig.drupipeSourcesController.loadedSources, "loadedSources", [debugMode: 'json'])
            def sourceDir = drupipeConfig.drupipeSourcesController.sourceDir(drupipeConfig.config, 'project')
            controller.drupipeLogger.trace "PROJECTS SOURCE DIR: ${sourceDir}"

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
                controller.drupipeLogger.trace "PROJECT FILE NAME TO CHECK: ${fileNameToCheck}"
                if (this.script.fileExists(fileNameToCheck)) {
                    controller.drupipeLogger.trace "SELECTING PROJECT FILE: ${fileNameToCheck}"
                    fileName = fileToCheck
                    break
                }
            }

            if (fileName != null) {
                projectConfig = utils.merge(projectConfig, drupipeConfig.drupipeSourcesController.sourceLoad(
                    sourceName: 'project',
                    configType: 'yaml',
                    configPath: fileName,
                ))
            }

            controller.drupipeLogger.debugLog(drupipeConfig.config, projectConfig, 'Project config', [debugMode: 'json'])

            if (projectConfig.config_version && projectConfig.config_version > 1 || controller.configVersion() > 1) {
                controller.drupipeLogger.log "Config version > 1"
                projectConfig = utils.merge(controller.drupipeConfig.config_version2(), projectConfig)
                controller.drupipeLogger.debugLog(drupipeConfig.config, projectConfig, 'Project config2', [debugMode: 'json'])
            }

            def projectConfigContext = utils.merge(drupipeConfig.config, projectConfig)

            def sources = [:]
            if (drupipeConfig.config.env.containsKey('UNIPIPE_SOURCES')) {
                controller.drupipeLogger.log "Processing UNIPIPE_SOURCES"
                def uniconfSourcesKey = utils.deepGet(projectConfigContext, 'uniconf.keys.sources')
                sources[uniconfSourcesKey] = script.readJSON(text: drupipeConfig.config.env['UNIPIPE_SOURCES'])
                if (projectConfig[uniconfSourcesKey]) {
                    projectConfig[uniconfSourcesKey] << sources[uniconfSourcesKey]
                }
                else {
                    projectConfig[uniconfSourcesKey] = sources[uniconfSourcesKey]
                }

                controller.drupipeLogger.debugLog(projectConfig, sources, 'UNIPIPE_SOURCES sources', ['debugMode': 'json'])
            }

            projectConfig = mergeScenariosConfigs(projectConfigContext, projectConfig, [:], 'project')

            controller.drupipeLogger.debugLog(drupipeConfig.config, projectConfig, 'Project config after mergeScenariosConfigs', [debugMode: 'json'])
        }
        projectConfig
    }

    def mergeScenariosConfigs(context, config, tempContext = [:], currentScenarioSourceName = null) {
        def uniconfIncludeKey = utils.deepGet(context, 'uniconf.keys.include')
        def uniconfSourcesKey = utils.deepGet(context, 'uniconf.keys.sources')

        controller.drupipeLogger.trace "uniconfIncludeKey: ${uniconfIncludeKey}"
        controller.drupipeLogger.trace "uniconfSourcesKey: ${uniconfSourcesKey}"

        if (config.containsKey('scenarios') && uniconfIncludeKey != 'scenarios') {
            config[uniconfIncludeKey] = config['scenarios']
        }

        def scenariosConfig = [:]
        if (!tempContext) {
            tempContext << config
        }

        tempContext = utils.merge(tempContext, config)
        if (config.containsKey(uniconfIncludeKey)) {
//            controller.drupipeLogger.log "config.containsKey(uniconfIncludeKey)"
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
//                    controller.drupipeLogger.log("scenarioSourceName: ${scenarioSourceName}")
//                    controller.drupipeLogger.log("scenario.name: ${scenario.name}")

                    controller.drupipeLogger.debugLog(config, tempContext[uniconfSourcesKey], 'Scenario sources', ['debugMode': 'json'])

                    if (
                    (scenariosConfig[uniconfSourcesKey] && scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || (tempContext[uniconfSourcesKey] && tempContext[uniconfSourcesKey].containsKey(scenarioSourceName))
                        || drupipeConfig.drupipeSourcesController.loadedSources.containsKey(scenarioSourceName)
                    )
                    {
                        if (!drupipeConfig.drupipeSourcesController.loadedSources[scenarioSourceName]) {
                            controller.drupipeLogger.log "Adding source: ${scenarioSourceName}"
                            if (tempContext[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = tempContext[uniconfSourcesKey][scenarioSourceName]
                            }
                            else if (scenariosConfig[uniconfSourcesKey].containsKey(scenarioSourceName)) {
                                scenario.source = scenariosConfig[uniconfSourcesKey][scenarioSourceName]
                            }

                            script.sshagent([drupipeConfig.config.credentialsId]) {
                                def source= [
                                    name: scenarioSourceName,
                                    type: 'git',
                                    path: ".unipipe/scenarios/${scenarioSourceName}",
                                    url: scenario.source.repo,
                                    branch: scenario.source.ref ? scenario.source.ref : 'master',
                                    mode: 'shell',
                                ]

                                drupipeConfig.drupipeSourcesController.sourceAdd(source)

//                                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], controller)
//                                controller.drupipeLogger.log "Source added: ${scenarioSourceName}"
                            }
                        }
                        else {
                            controller.drupipeLogger.debugLog(config, "Source: ${scenarioSourceName} already added")
                            scenario.source = drupipeConfig.drupipeSourcesController.loadedSources[scenarioSourceName]
                        }

                        def fileName = null

                        def sourceDir = drupipeConfig.drupipeSourcesController.sourceDir(config, scenarioSourceName)

                        // TODO: recheck it.
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
                            controller.drupipeLogger.debug "Scenario file name: ${fileName} exists"
                            def scenarioConfig = mergeScenariosConfigs(context, script.readYaml(file: fileName), tempContext, scenarioSourceName)
                            controller.drupipeLogger.debug "Loaded scenario: ${scenarioSourceName}:${scenario.name} config"
                            scenariosConfig = utils.merge(scenariosConfig, scenarioConfig)
                            controller.drupipeLogger.debugLog(config, scenariosConfig, "Scenarios config")
                        }
                        else {
                            controller.drupipeLogger.warning "Scenario file doesn't found"
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
