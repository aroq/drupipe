#!groovy

// Pipeline used to create project specific pipelines.
def call(LinkedHashMap p = [:]) {
    drupipe { context ->
        drupipeBlock(withDocker: true, nodeName: 'default', context) {
            checkout scm
            drupipeAction(action: 'Docman.info', context)
            stash name: 'config', includes: 'docroot/config/**, library/**, mothership/**', excludes: '.git, .git/**'
        }

        drupipeBlock(nodeName: 'master', context) {
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
                context.defaultActionParams.JobDslSeed_perform.jobsPattern << 'docroot/config/pipelines/jobdsl/*.groovy'
            }
            drupipeAction(action: 'JobDslSeed.perform', context)
        }
    }
}
