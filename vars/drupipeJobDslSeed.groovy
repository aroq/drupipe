#!groovy

// Pipeline used to create project specific pipelines.
def call(LinkedHashMap p = [:]) {
    drupipe { pipeline ->
        drupipeBlock(withDocker: true, nodeName: 'default', dockerImage: pipeline.context.defaultDocmanImage, pipeline) {
            checkout scm
            drupipeAction([action: 'Docman.info'], pipeline)
            def stashes = pipeline.context.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')
            stashes = stashes + ", ${pipeline.context.docmanDir}/config/config.json"
            stash name: 'config', includes: "${stashes}", excludes: '.git, .git/**'
        }

        drupipeBlock([nodeName: 'master'], pipeline) {
            checkout scm
            if (fileExists(pipeline.context.projectConfigPath)) {
                dir(pipeline.context.projectConfigPath) {
                    deleteDir()
                }
                dir('.unipipe/library') {
                    deleteDir()
                }
                dir('.unipipe/mothership') {
                    deleteDir()
                }
            }

            unstash 'config'
            if (fileExists("${pipeline.context.projectConfigPath}/pipelines/jobdsl")) {
                pipeline.context.params.action.JobDslSeed_perform.jobsPattern << "${pipeline.context.projectConfigPath}/pipelines/jobdsl/*.groovy"
            }
            drupipeAction([action: 'JobDslSeed.perform'], pipeline)
        }
    }
}
