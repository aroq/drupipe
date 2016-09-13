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
                            action: 'Docman.config',
                        ],
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
    }
}

