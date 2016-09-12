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
                            name: 'config',
                            type: 'dir',
                            path: '',
                        ]
                    ]
                ],
                [
                    action: 'Config.perform',
                    params: [
                        configProviders: [
                            action: 'Source.loadConfig',
                            params: [
                                sourceName: 'config',
                                configType: 'groovy',
                            ]
                        ]
                    ]
                ]
            ],
            'seed': [
//                [
//                    action: 'JobDslSeed.perform',
//                ],
            ],
        ]
        p = params
    }
    params
}
