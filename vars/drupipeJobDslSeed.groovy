#!groovy

def call(body) {
    // Pipeline used to create project specific pipelines.
    withDrupipeDocker() {
        // it = commandParams from body(commandParams)
        params << it
        checkout scm
        parameters = executePipelineAction(action: 'Docman.info', params)

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
        if (fileExists('docroot/config/pipelines/jobdsl')) {
            parameters.actionParams.JobDslSeed_perform.jobsPattern << 'docroot/config/pipelines/jobdsl/*.groovy'
        }
        executePipelineAction(action: 'JobDslSeed.perform', parameters)
    }
}
