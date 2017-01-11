#!groovy

// Pipeline used to create project specific pipelines.
def call(body) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                checkout scm
                drupipeAction(action: 'Docman.info', config)

                stash name: 'config', includes: 'docroot/config/**, library/**, mothership/**', excludes: '.git, .git/**'

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
            drupipeAction(action: 'JobDslSeed.perform', config)
        }
    }
}
