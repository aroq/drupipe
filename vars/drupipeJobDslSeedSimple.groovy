#!groovy

// Pipeline used to create project specific pipelines.
def call(LinkedHashMap p = [:]) {
    drupipe { context ->
        drupipeBlock(withDocker: true, nodeName: 'default', dockerImage: 'michaeltigr/zebra-build-php-drush-docman', context) {
            checkout scm
            stash name: 'config', includes: "${context.projectConfigPath}/**, library/**, mothership/**', excludes: '.git, .git/**"
        }

        drupipeBlock(nodeName: 'master', context) {
            checkout scm
            if (fileExists(context.projectConfigPath)) {
                dir(context.projectConfigPath) {
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
            if (fileExists("${context.projectConfigPath}/pipelines/jobdsl")) {
                context.defaultActionParams.JobDslSeed_perform.jobsPattern << "${context.projectConfigPath}/pipelines/jobdsl/*.groovy"
            }
            drupipeAction(action: 'JobDslSeed.perform', context)
        }
    }
}
