def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        checkout scm

        params << executeStage('init') {
            p = params
            actions = ['Library.perform', 'Config.perform']
        }

//        config = initStage {
//            p = params
//        }

        stage 'seed'

        jobDsl targets: [params.jobsPattern].join('\n'),
               removedJobAction: 'DELETE',
               removedViewAction: 'DELETE',
               lookupStrategy: 'SEED_JOB',
               additionalClasspath: ['library/src'].join('\n')
    }
}
