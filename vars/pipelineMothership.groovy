def call() {
    drupipePipeline(
        blocks:
            [
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
            ]
    )
}
