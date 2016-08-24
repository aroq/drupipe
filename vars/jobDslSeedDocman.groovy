def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        checkout scm

        config = initStage {
            paramsTest = params
        }

        stage 'seed'

        jobDsl targets: [config.jobsPattern].join('\n'),
               removedJobAction: 'DELETE',
               removedViewAction: 'DELETE',
               lookupStrategy: 'SEED_JOB',
               additionalClasspath: ['library/src'].join('\n')
    }
}
