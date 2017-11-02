#!groovy

// Pipeline used to create project specific pipelines.
def call(LinkedHashMap p = [:]) {
    drupipe { pipeline ->
        drupipeBlock([nodeName: 'master', 'name': 'clean-workspace'], pipeline) {
            checkout scm
            if (fileExists(pipeline.context.projectConfigPath)) {
                dir(pipeline.context.projectConfigPath) {
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
            if (fileExists("${pipeline.context.projectConfigPath}/pipelines/jobdsl")) {
                pipeline.context.params.action.JobDslSeed_perform.jobsPattern << "${pipeline.context.projectConfigPath}/pipelines/jobdsl/*.groovy"
            }
            drupipeAction([action: 'JobDslSeed.perform'], pipeline)
        }
    }
}
