def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        checkout scm

        stage 'config'

    //    dir('automation') {
    //      git url: 'https://github.com/...git', branch: 'master'
    //    }

        def configHelper = new com.github.aroq.workflowlibs.Config()
        def config = configHelper.readGroovyConfig(params.configFileName)
        config << params

        if (config.configProvider == 'docman') {
            def docman = new com.github.aroq.workflowlibs.Docman()
            docman.info(config)
        }

        stage 'seed'

        jobDsl targets: [config.jobsPattern].join('\n'),
               removedJobAction: 'DELETE',
               removedViewAction: 'DELETE',
               lookupStrategy: 'SEED_JOB',
    //           additionalClasspath: ['automation/src/main/groovy'].join('\n')
               additionalClasspath: ['src/main/groovy'].join('\n')
    }
}
