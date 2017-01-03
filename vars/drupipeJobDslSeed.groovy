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
    drupipe() {
        // it - commandParams from body(commandParams)
        params << it
        node(commandParams.nodeName) {
            withDrupipeDocker(params) {
                // it - commandParams from body(commandParams)
                params << it
                checkout scm
                parameters = executePipelineAction(action: 'Docman.info', params)

                stash name: 'config', includes: 'docroot/config/**, library/**, mothership/**', excludes: '.git, .git/**'

                parameters
            }
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
            if (fileExists('docroot/config/pipelines/jobdsl')) {
                parameters.actionParams.JobDslSeed_perform.jobsPattern << 'docroot/config/pipelines/jobdsl/*.groovy'
            }
            executePipelineAction(action: 'JobDslSeed.perform', parameters)
        }
    }

}
