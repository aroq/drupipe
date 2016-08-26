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
                            action: 'Config.perform',
                            params: [
                                configProvider: 'docman',
                                configFileName: 'docroot/config/docroot.config'
                            ]
                        ],
                        [
                            action: 'Oper.perform',
                            params: [param1: 'test2']
                        ]
                    ],
                    'build': [
                        [
                            action: 'Docman.deploy',
                            params: [param1: 'test2']
                        ],
                    ]
                ]
            p = params
        }
        dump(config)
    }
}

