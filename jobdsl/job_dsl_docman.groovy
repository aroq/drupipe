import com.github.aroq.DocmanConfig

def configFilePath = 'docroot/config/docroot.config'
def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace(configFilePath))

docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : 'docroot/config/config.json'

docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

// Retrieve Docman config from json file (prepared by "docman info" command).
def docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)

// TODO: Use docman config to retrieve info.
branches = [
    development: [
        pipeline: 'deploy',
        quietPeriodSeconds: 5,
    ],
    staging: [
        pipeline: 'deploy',
        quietPeriodSeconds: 5,
    ],
    stable: [
        pipeline: 'release',
        quietPeriodSeconds: 5,
    ],
]

if (config.branches) {
  branches = merge(branches, config.branches)
}

// Create pipeline jobs for each state defined in Docman config.
docmanConfig.states?.each { state ->
    def params = config
    println "Processing state: ${state.key}"
    if (branches[state.key]) {
        params = merge(config, branches[state.key])
    }
    if (branches[state.key]?.branch) {
        branch = branches[state.key]?.branch
    }
    else {
        branch = docmanConfig.getVersionBranch('', state.key)
    }
    println "Params: ${params}"
    println "DocmanConfig: getVersionBranch: ${branch}"
    buildEnvironment = docmanConfig.getEnvironmentByState(state.key)
    println "Environment: ${buildEnvironment}"
    pipelineJob(state.key) {
        if (params.quietPeriodSeconds) {
            quietPeriod(params.quietPeriodSeconds)
        }
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            stringParam('projectName', '')
            stringParam('debugEnabled', '0')
            stringParam('force', '0')
            stringParam('simulate', '0')
            stringParam('docrootDir', 'docroot')
            stringParam('config_repo', params.configRepo)
            stringParam('type', 'branch')
            stringParam('environment', buildEnvironment)
            stringParam('version', branch)
        }
        definition {
            cpsScm {
                scm {
                    git() {
                        remote {
                            name('origin')
                            url(params.configRepo)
                            credentials(params.credentialsID)
                        }
                        extensions {
                            relativeTargetDirectory('docroot/config')
                        }
                    }
                    scriptPath("docroot/config/pipelines/${params.pipeline}.groovy")
                }
            }
        }
        triggers {
            gitlabPush {
                buildOnPushEvents()
                buildOnMergeRequestEvents(false)
                enableCiSkip()
                useCiFeatures()
                includeBranches(branch)
            }
        }
        properties {
            gitLabConnectionProperty {
                gitLabConnection('Gitlab')
            }
        }
    }
}

Map merge(Map[] sources) {
    if (sources.length == 0) return [:]
    if (sources.length == 1) return sources[0]

    sources.inject([:]) { result, source ->
        source.each { k, v ->
            result[k] = result[k] instanceof Map ? merge(result[k], v) : v
        }
        result
    }
}
