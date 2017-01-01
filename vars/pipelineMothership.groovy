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
                            action: 'Source.add',
                            params: [
                                source: [
                                      name: 'library',
                                      type: 'git',
                                      path: 'library',
                                      url: params.drupipeLibraryUrl,
                                      branch: params.drupipeLibraryBranch,
                                ]
                            ]
                        ],
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

