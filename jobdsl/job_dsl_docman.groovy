// Init params
def params = [:]
configFilePath = 'config/docroot.config'
def config

config = ConfigSlurper.newInstance(params.environment).parse(readFileFromWorkspace(configFilePath))
deployPipeline = readFileFromWorkspace(config.pipeline)
releasePipeline = readFileFromWorkspace(config.releasePipeline)
mergePipeline = readFileFromWorkspace(config.mergePipeline)

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
        cps {
            script(releasePipeline)
            sandbox()
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
            script(releasePipeline)
            sandbox()
        }
    }
    triggers {
        gitlabPush {
            buildOnPushEvents(false)
            buildOnMergeRequestEvents(true)
            enableCiSkip()
            useCiFeatures()
            includeBranches('master')
        }
    }
    properties {
        gitLabConnectionProperty {
            gitLabConnection('Gitlab')
        }
    }
}
