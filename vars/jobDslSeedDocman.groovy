def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    executePipeline {
        checkoutSCM = true
        pipeline = [
            'init': [
                [
                    action: 'Source.add',
                    params: [
                        source: [
                            name: 'docmanConfig',
                            type: 'dir',
                            path: 'config',
                        ]
                    ]
                ],
                [
                    action: 'Config.perform',
                    params: [
                        configProviders: [
                            action: 'Source.loadConfig',
                            params: [
                                sourceName: 'docmanConfig',
                                configType: 'groovy',
//                                configPath: 'docroot.config'
                            ]
                        ]
                    ]
                ]
            ],
            'seed': [
                [
                    action: 'JobDslSeed.perform',
                ],
            ],
        ]
        p = params
    }
}
