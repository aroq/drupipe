#!groovy

def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    if (params.params) {
        params << params.params
        params.remove('params')
    }

    // Pipeline used to create project specific pipelines.
    withDrupipeDocker() {
        params << it
        parameters = pipelineJobDslSeedDocman {
            configPath = 'docroot.config'
            params = params
        }
        stash name: 'config', includes: 'docroot/config/**, library/**, mothership/**', excludes: '.git, .git/**'
        parameters
    }

    node('master') {
        if (fileExists('docroot/config')) {
            dir('docroot/config') {
                deleteDir()
            }
            dir('library') {
                deleteDir()
            }
            dir('mothership') {
                deleteDir()
            }
        }

        unstash 'config'
        parameters.actionParams.JobDslSeed_perform.jobsPattern << 'docroot/config/pipelines/jobdsl/*.groovy'
        executePipelineAction(action: 'JobDslSeed.perform', parameters)
    }

}
