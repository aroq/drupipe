def call() {
    drupipe { pipeline ->
        drupipeBlock(nodeName: 'master', pipeline) {
            drupipeStage('seed', pipeline) {
                drupipeAction(
                    action: 'JobDslSeed.perform',
                    params: [
                        lookupStrategy: 'JENKINS_ROOT',
                        jobsPattern: ['library/jobdsl/job_dsl_mothership.groovy'],
                        override: true,
                        removedJobAction: 'DELETE',
                        removedViewAction: 'DELETE',
                        additionalClasspath: ['library/src'],
                    ],
                    pipeline
                )
            }
        }
    }
}
