def call() {
    drupipe { context ->
        drupipeBlock(nodeName: 'master', context) {
            drupipeStages([
                seed: [
                    [
                        action: 'JobDslSeed.perform',
                        params: [
                            lookupStrategy: 'JENKINS_ROOT',
                            jobsPattern: ['library/jobdsl/job_dsl_mothership.groovy'],
                        ],
                    ],
                ]
            ], context)
        }
    }
}
