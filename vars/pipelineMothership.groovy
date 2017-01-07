def call(body) {
    // def params = [:]
    // body.resolveStrategy = Closure.DELEGATE_FIRST
    // body.delegate = params
    // body()

    // node('master') {
        // def drupipePipeline =
            // [
                // 'seed': [
                    // [
                        // action: 'JobDslSeed.perform',
                        // params: [
                            // lookupStrategy: 'JENKINS_ROOT',
                            // jobsPattern: ['library/jobdsl/job_dsl_mothership.groovy'],
                        // ]
                    // ],
                // ],
            // ]

        // drupipeStages([
            // pipeline: drupipePipeline
        // ], params)
    // }

    drupipePipeline(
        stages:
            [
                seed: [
                    [
                        action: 'JobDslSeed.perform',
                        params: [
                            lookupStrategy: 'JENKINS_ROOT',
                            jobsPattern: ['library/jobdsl/job_dsl_mothership.groovy'],
                        ],
                    ]
                ],
            ],
        params: [nodeName: 'master', drupipeDocker: false]
    )
}
