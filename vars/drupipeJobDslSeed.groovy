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
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                checkout scm
                parameters = executePipelineAction(action: 'Docman.info', config)

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
                config.actionParams.JobDslSeed_perform.jobsPattern << 'docroot/config/pipelines/jobdsl/*.groovy'
            }
            executePipelineAction(action: 'JobDslSeed.perform', config)
        }
    }

}
