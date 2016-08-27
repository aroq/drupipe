def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        config = executePipeline {
            pipeline =
                [
                    'init': [
                        [
                            action: 'Docman.info',
                        ],
                        [
                            action: 'Config.perform',
                            params: [
                                configProviders: [action: 'GroovyFileConfig.load', params: [configFileName: 'docroot/config/docroot.config']],
                            ]
                        ],
                        [
                            action: 'Oper.perform',
                        ]
                    ],
                    'build': [
                        [
                            action: 'Docman.deploy',
                        ],
                    ],
                    'ops': [
                        [
                            action: 'Druflow.deployFlow',
                        ],
                    ]
                ]
            p = params
        }
        dump(config, 'pipeline result')
    }
}

