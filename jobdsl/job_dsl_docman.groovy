import com.github.aroq.DocmanConfig

configFilePath = 'config/docroot.config'
def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace(configFilePath))

docrootConfigJson = readFileFromWorkspace(config.docrootConfigJsonPath)

// Retrieve Docman config from json file (prepared by "docman info" command).
def docmanConfig = new DocmanConfig(docrootConfigJson: docrootConfigJson)

// TODO: Use docman config to retrieve info.
def branches = [
    development: [
        branch: 'develop',
        pipeline: 'deploy',
        environment: 'dev',
    ],
    staging: [
        branch: 'master',
        pipeline: 'deploy',
        environment: 'test',
    ],
    stable: [
        branch: 'state_stable',
        pipeline: 'release',
    ],
]

// Create pipeline jobs for each state defined in Docman config.
docmanConfig.states?.each { state ->
    pipelineJob(state.key) {
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            stringParam('executeCommand', 'deployFlow')
            stringParam('projectName', '')
            stringParam('debug', '0')
            stringParam('force', '0')
            stringParam('simulate', '0')
            stringParam('docrootDir', 'docroot')
            stringParam('config_repo', config.configRepo)
            stringParam('type', 'branch')
            if (branches[state.key]?.environment) {
              stringParam('environment', branches[state.key]?.environment)
            }
            stringParam('version', branches[state.key]?.branch)
        }
        definition {
            cpsScm {
                scm {
                    git() {
                        remote {
                            name('origin')
                            url(config.configRepo)
                            credentials(config.credentialsId)
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

//pipelineJob("merge") {
//    concurrentBuild(false)
//    logRotator(-1, 30)
//    parameters {
//        stringParam('executeCommand', 'deployFlow')
//        stringParam('projectName', 'common')
//        stringParam('debug', '0')
//        stringParam('force', '0')
//        stringParam('simulate', '0')
//        stringParam('docrootDir', 'docroot')
//        stringParam('config_repo', config.configRepo)
//        stringParam('type', 'branch')
//        stringParam('version', 'state_stable')
//    }
//    definition {
//        cps {
//            script(mergePipeline)
//            sandbox()
//        }
//    }
//    triggers {
//        gitlabPush {
//            buildOnPushEvents(false)
//            buildOnMergeRequestEvents(true)
//            enableCiSkip()
//            useCiFeatures()
//        }
//    }
//    properties {
//        gitLabConnectionProperty {
//            gitLabConnection('Gitlab')
//        }
//    }
//}
