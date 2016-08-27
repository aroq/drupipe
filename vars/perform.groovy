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
                            action: 'Source.add',
                            params: [
                                source: [
                                    name: 'docmanDocroot',
                                    type: 'docmanDocroot',
                                    path: 'docroot',
                                ]
                            ]
                        ],
                        [
                            action: 'Config.perform',
                            params: [
                                configProviders: [
                                    action: 'Source.loadConfig',
                                    params: [
                                        sourceName: 'docmanDocroot',
                                        configType: 'groovy',
                                        configPath: 'config/docroot.config'
                                    ]
                                ]
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

