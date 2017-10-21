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
            [
                action: "Config.jobConfig"
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

        // For compatibility:
        if (context.defaultActionParams) {
            context.params.action = utils.merge(context.params.action, context.defaultActionParams)
        }

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
                // For compatibility:
                context.params.action = utils.merge(context.params.action, context.environmentParams.defaultActionParams)

                context.params.action = utils.merge(context.params.action, context.params.action)

                utils.jsonDump(context, context.environmentParams, 'ENVIRONMENT PARAMS')
            }
        }

        utils.debugLog(context, context, 'CONFIG CONTEXT')

        context.drupipeShellReturnStdout = false

        def stashes = context.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')

        script.echo "Stashes: ${stashes}"

        script.stash name: 'config', includes: "${stashes}", excludes: ".git, .git/**"
        context
    }

    def jenkinsConfig() {
        action.params.jenkinsParams
    }

    def jobConfig() {
        def result = [:]
        if (context.jobs) {
            processJobs(context.jobs)

            util.jsonDump(context, context.jobs, 'CONFIG JOBS PROCESSED')

            result.job = (context.env.JOB_NAME).split('/').drop(1).inject(context, { obj, prop ->
                obj.jobs[prop]
            })

            if (result.job) {
                if (result.job.context) {
                    result = utils.merge(result, result.job.context)
                }
            }
            result.jobs = context.jobs
        }
        result
    }

    def processJobs(jobs, prefixes = [], parentParams = [:]) {
        if (jobs) {
            for (job in jobs) {
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

//    def jobConfig() {
//        def result = [:]
//        if (context.jobs) {
//            def jobs = processJobs(context, context.jobs)
//            result.jobs_processed = jobs
//            result.job = jobs[context.env.JOB_NAME]
//            if (result.job) {
//                utils.jsonDump(context, result.job, 'JOB')
//                if (result.job.context) {
//                    result = utils.merge(result, job.context)
//                }
//            }
//        }
//        result
//    }

//    def processJobs(jobs, prefixes = []) {
//        if (jobs) {
//            for (i = 0; i < jobs.size(); i++) {
//                def children = jobs[i].value.children ? jobs[i].value.children : [:]
//                jobs[i] = getJobConfigByName([prefixes << jobs[i].key].join('/'))
//                if (children) {
//                    processJobs(children, [prefixes << jobs[i].key])
//                }
//            }
//        }
//    }

//    def processJobs(context, jobs, prefixes = [], params = [:]) {
//        def result = [:]
//        if (jobs) {
//            for (job in jobs) {
//              println "job: ${job}"
//                def children = job.value.children ? job.value.children : [:]
//                def jobKey = (prefixes.clone() << job.key).join('/')
//                result[jobKey] = merge(params, job.value)
//                if (children) {
//                    result << processJobs(context, children, prefixes << job.key, job.value)
//                }
//                job = result[jobKey]
//            }
//        }
//        result
//    }
//
//    def getJobConfigByName(String name) {
//        def parts = name.split('/').drop(1)
//        getJobConfig(context.jobs, parts, 0, [:])
//    }
//
//    def getJobConfig(jobs, parts, counter = 0, r = [:]) {
//        script.echo "Counter: ${counter}"
//        def part = parts[counter]
//        script.echo "Part: ${part}"
//        def j = jobs[part] ? jobs[part] : [:]
//        if (j) {
//            def children = j.containsKey('children') ? j['children'] : [:]
//            j.remove('children')
//            r = utils.merge(r, j)
//            if (children) {
//                getJobConfig(children, parts, counter + 1, r)
//            }
//            else {
//                r
//            }
//        }
//        else {
//            [:]
//        }
//    }


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
            def mothershipConfig = this.utils.getMothershipConfigFile(context)
            def mothershipServers = this.utils.getMothershipServersFile(context)

            result = utils.merge(result, mothershipConfig[context.jenkinsFolderName])
            result = utils.merge(result, [jenkinsServers: mothershipServers])

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
                            utils.debugLog(context, "Source: ${scenarioSourceName} already added")
                            scenario.source = context.loadedSources[scenarioSourceName]
                        }

                        def fileName = null

                        def sourceDir = utils.sourceDir(context, scenarioSourceName)

                        def filesToCheck = [
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
                            utils.debugLog(context, "Scenario file name: ${fileName} exists")
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
        utils.debugLog(context, "projectConfig repo: ${context.configRepo}")
        if (context.configRepo) {
            def sourceObject = [
                name: 'project',
                path: 'sources/project',
                type: 'git',
                url: context.configRepo,
                branch: 'master',
                mode: 'shell',
            ]

            script.sshagent([context.credentialsId]) {
                this.script.drupipeAction([action: "Source.add", params: [source: sourceObject]], context)
            }

            def providers = [
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'project',
                        configType: 'groovy',
                        configPath: context.projectConfigFile
                    ]
                ],
            ]

            def fileName = null
            def sourceDir = utils.sourceDir(context, 'project')
            this.script.echo("PROJECTS SOURCE DIR: ${sourceDir}")

            def filesToCheck = [
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
