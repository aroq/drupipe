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
                            action: 'DocmanConfig.perform',
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

