def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node('master') {
        config = executePipeline {
            noNode = true
            pipeline =
                [
                    'seed': [
                        [
                            action: 'JobDslSeed.perform',
                            params: [
                                lookupStrategy: 'JENKINS_ROOT',
                                jobsPattern: ['library/jobdsl/job_dsl_mothership.groovy'],
                            ]
                        ],
                    ],
                ]
            p = params
        }
    }
}

