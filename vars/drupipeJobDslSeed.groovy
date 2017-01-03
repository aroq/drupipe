#!groovy

// Pipeline used to create project specific pipelines.
def call(body) {
    echo "1"
    drupipe() { config ->
        echo "2"
        node(config.nodeName) {
            echo "3"
            withDrupipeDocker(config) {
                echo "4"
                checkout scm
                executePipelineAction(action: 'Docman.info', config)

                stash name: 'config', includes: 'docroot/config/**, library/**, mothership/**', excludes: '.git, .git/**'

            }
        }

        echo "5"

        node('master') {
            echo "6"
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
