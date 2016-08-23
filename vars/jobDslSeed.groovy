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

        def configHelper = new com.github.aroq.jenkins.workflowlibs.Config()
        def config = configHelper.readGroovyConfig(params.configFileName)
        config << params
        echo config.configRepo

        if (config.configProvider == 'docman') {
            def docmanHelper = new com.github.aroq.docman.Docman()
            docmanHelper.info(config)
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
