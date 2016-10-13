import com.github.aroq.DocmanConfig

configFilePath = 'config/docroot.config'
def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace(configFilePath))
mergePipeline = readFileFromWorkspace(config.mergePipeline)

docrootConfigJson = readFileFromWorkspace(config.docrootConfigJsonPath)

// Retrieve Docman config from json file (prepared by "docman info" command).
def docmanConfig = new DocmanConfig(docrootConfigJson: docrootConfigJson)

def branches = [
    development: [
        'branch': 'develop',
    ],
    staging: [
        'branch': 'master',
    ],
    stable: [
        'branch': 'state_stable',
    ],
]

// Create pipeline jobs for each state defined in Docman config.
docmanConfig.states?.each { state ->
    pipelineJob(state.key) {
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            stringParam('executeCommand', 'deployFlow')
            stringParam('projectName', 'common')
            stringParam('debug', '0')
            stringParam('force', '0')
            stringParam('simulate', '0')
            stringParam('docrootDir', 'docroot')
            stringParam('config_repo', config.configRepo)
            stringParam('type', 'branch')
            stringParam('version', branches[state.key]?.branch)
        }
        definition {
            cpsScm {
                scm {
                    git() {
                        remote {
                            name('origin')
                            url(config.configRepo)
                        }
                        extensions {
                            relativeTargetDirectory('docroot/config')
                        }
                    }
                    scriptPath('docroot/config/pipelines/release.groovy')
                }
            }
        }
        triggers {
            gitlabPush {
                buildOnPushEvents()
                buildOnMergeRequestEvents(false)
                enableCiSkip()
                useCiFeatures()
                includeBranches('state_stable')
            }
        }
        properties {
            gitLabConnectionProperty {
                gitLabConnection('Gitlab')
            }
        }
    }
}

pipelineJob("release") {
    concurrentBuild(false)
    logRotator(-1, 30)
    parameters {
        stringParam('executeCommand', 'deployFlow')
        stringParam('projectName', 'common')
        stringParam('debug', '0')
        stringParam('force', '0')
        stringParam('simulate', '0')
        stringParam('docrootDir', 'docroot')
        stringParam('config_repo', config.configRepo)
        stringParam('type', 'branch')
        stringParam('version', 'state_stable')
    }
    definition {
        cpsScm {
            scm {
                git() {
                    remote {
                        name('origin')
                        url(config.configRepo)
                    }
                    extensions {
                        relativeTargetDirectory('docroot/config')
                    }
                }
                scriptPath('docroot/config/pipelines/release.groovy')
            }
        }
    }
    triggers {
        gitlabPush {
            buildOnPushEvents()
            buildOnMergeRequestEvents(false)
            enableCiSkip()
            useCiFeatures()
            includeBranches('state_stable')
        }
    }
    properties {
        gitLabConnectionProperty {
            gitLabConnection('Gitlab')
        }
    }
}

pipelineJob("merge") {
    concurrentBuild(false)
    logRotator(-1, 30)
    parameters {
        stringParam('executeCommand', 'deployFlow')
        stringParam('projectName', 'common')
        stringParam('debug', '0')
        stringParam('force', '0')
        stringParam('simulate', '0')
        stringParam('docrootDir', 'docroot')
        stringParam('config_repo', config.configRepo)
        stringParam('type', 'branch')
        stringParam('version', 'state_stable')
    }
    definition {
        cps {
            script(mergePipeline)
            sandbox()
        }
    }
    triggers {
        gitlabPush {
            buildOnPushEvents(false)
            buildOnMergeRequestEvents(true)
            enableCiSkip()
            useCiFeatures()
        }
    }
    properties {
        gitLabConnectionProperty {
            gitLabConnection('Gitlab')
        }
    }
}
