import com.github.aroq.DocmanConfig

// Init params
def params = [:]
configFilePath = 'config/docroot.config'
def config

if (!System.properties.docrootConfigJsonPath) {
    BUILD_NUMBER = 'pipeline'
}

// Determine current environment.
try {
    BUILD_NUMBER
    params.environment = 'jenkins'
    config = ConfigSlurper.newInstance(params.environment).parse(readFileFromWorkspace(configFilePath))
    docrootConfigJson = readFileFromWorkspace(config.docrootConfigJsonPath)
    deployPipeline = readFileFromWorkspace(config.pipeline)
    triggerPipeline = readFileFromWorkspace(config.triggerPipeline)
}
catch (MissingPropertyException mpe) {
    params.environment = 'local'
    config = ConfigSlurper.newInstance(params.environment).parse(new File(configFilePath).text)
    docrootConfigJson = new File(System.properties.docrootConfigJsonPath).text
    deployPipeline = new File(config.pipeline).text
    triggerPipeline = new File(config.triggerPipeline).text
}

// Create folder for trigger jobs.
folder("${config.baseFolder}")
folder("${config.baseFolder}/tools")
folder("${config.baseFolder}/tools/${config.triggersFolder}")

// Retrieve Docman config from json file (prepared by "docman info" command).
def docmanConfig = new DocmanConfig(docrootConfigJson: docrootConfigJson)

// Create pipeline jobs for each state defined in Docman config.
docmanConfig.states?.each { state ->
    pipelineJob("${config.baseFolder}/${state.key}") {
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            stringParam('executeCommand', 'deployFlow')
            stringParam('projectName', 'common')
            stringParam('environment', state.value)
            stringParam('debug', '0')
            stringParam('simulate', '0')
            stringParam('docrootDir', 'docroot')
            stringParam('config_repo', config.configRepo)
            stringParam('type', 'branch')
            stringParam('version', 'develop')
            stringParam('force', '0')
            stringParam('skip_stage_build', '0')
            stringParam('skip_stage_operations', '0')
            stringParam('skip_stage_test', '0')
        }
        definition {
            cps {
                // See the pipeline script.
                script(deployPipeline)
                sandbox()
            }
        }
    }
}

pipelineJob("${config.baseFolder}/trigger") {
    concurrentBuild(false)
    logRotator(-1, 30)
    definition {
        cps {
            // See the pipeline script.
            script(triggerPipeline)
            sandbox()
        }
    }
}

// Create trigger jobs for each project & state defined in Docman config.
docmanConfig.projects?.each { project ->
    if (project.value['repo'] && project.value['trigger'] != false) {
        project.value['states']?.each { stateName, state ->
            String versionBranch = docmanConfig.getVersionBranch(project.key, stateName)
            job("${config.baseFolder}/tools/${config.triggersFolder}/${project.key}-${stateName}") {
                logRotator(-1, 30)
                scm() {
                    git {
                        remote {
                            url(project.value['repo'])
                        }
                        branch("*/${versionBranch}")
                    }
                }
                steps {
                    downstreamParameterized {
                        // Trigger pipeline job for this state.
                        trigger(config.baseFolder + '/' + stateName) {
                            parameters {
                                currentBuild()
                                predefinedProp('projectName', project.key)
                                predefinedProp('version', versionBranch)
                            }
                        }
                    }
                }
            }
        }
    }
}
