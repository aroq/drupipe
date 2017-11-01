#!groovy

// Pipeline used to create project specific pipelines.
def call(LinkedHashMap p = [:]) {
    drupipe { context, pipeline ->
        drupipeBlock(withDocker: true, nodeName: 'default', dockerImage: context.defaultDocmanImage, pipeline) {
            checkout scm
            drupipeAction(action: 'Docman.info', pipeline)
            stash name: 'config', includes: "${context.projectConfigPath}/**, library/**, mothership/**', excludes: '.git, .git/**"
        }

        drupipeBlock(nodeName: 'master') {
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
                context.params.action.JobDslSeed_perform.jobsPattern << "${context.projectConfigPath}/pipelines/jobdsl/*.groovy"
            }
            drupipeAction(action: 'JobDslSeed.perform', context)
        }
    }
}
