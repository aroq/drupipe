def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        config = executePipeline {
            pipeline =
                [
                    'seed': [
                        [
                            action: 'JobDSLSeed.perform',
                        ],
                    ],
                ]
            p = params
        }
    }
}

