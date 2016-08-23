def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    node {
        checkout scm

        stage 'config'

    //    dir('automation') {
    //      git url: 'https://github.com/...git', branch: 'master'
    //    }

        def configHelper = new com.github.aroq.jenkins.workflowlibs.Config()
        def fileConfig = configHelper.readGroovyConfig(config.configFileName)

        def tempConfig = fileConfig
        tempConfig << config
        config = tempConfig
        echo config.configRepo

//        def config_defaults = [force: '0', configProvider: 'docman']
//        config << readProperties(defaults: config_defaults, file: config.configFileName)

        if (config.configProvider == 'docman') {
            echo "Requesting docman for config..."
            sh(
            """#!/bin/bash -l
            if [ "${force}" == "1" ]; then
              FLAG="-f"
              rm -fR docroot
            fi
            docman init docroot ${config.configRepo} -s
            cd docroot
            docman info full config.json
            """
            )
            echo "Requesting docman for config... DONE."
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
