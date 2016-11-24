import com.github.aroq.DocmanConfig

def configFilePath = 'docroot/config/docroot.config'
def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace(configFilePath))

docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : 'docroot/config/config.json'

docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

// Retrieve Docman config from json file (prepared by "docman info" command).
def docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)

// TODO: Use docman config to retrieve info.
def branches = [
    development: [
        pipeline: 'deploy',
        environment: 'dev',
    ],
    staging: [
        pipeline: 'deploy',
        environment: 'test',
    ],
    stable: [
        pipeline: 'release',
    ],
]

// Create pipeline jobs for each state defined in Docman config.
docmanConfig.states?.each { state ->
    println "Processing state: ${state.key}"
    branch = docmanConfig.getVersionBranch('rh', state.key)
    println "DocmanConfig: getVersionBranch: ${branch}"
    environment = docmanConfig.getEnvironmentByState(state.key)
    prinltn "Environment: ${environment}".toString()
    pipelineJob(state.key) {
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            stringParam('projectName', '')
            stringParam('debugEnabled', '0')
            stringParam('force', '0')
            stringParam('simulate', '0')
            stringParam('docrootDir', 'docroot')
            stringParam('config_repo', config.configRepo)
            stringParam('type', 'branch')
            if (branches[state.key]?.environment) {
              stringParam('environment', branches[state.key]?.environment)
            }
            stringParam('version', branch)
        }
        definition {
            cpsScm {
                scm {
                    git() {
                        remote {
                            name('origin')
                            url(config.configRepo)
                            credentials(config.credentialsID)
                        }
                        extensions {
                            relativeTargetDirectory('docroot/config')
                        }
                    }
                    scriptPath("docroot/config/pipelines/${branches[state.key]?.pipeline}.groovy")
                }
            }
        }
        triggers {
            gitlabPush {
                buildOnPushEvents()
                buildOnMergeRequestEvents(false)
                enableCiSkip()
                useCiFeatures()
                includeBranches(branches[state.key]?.branch)
            }
        }
        properties {
            gitLabConnectionProperty {
                gitLabConnection('Gitlab')
            }
        }
    }
}
