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
                            action: 'JobDslSeed.perform',
                        ],
                    ],
                ]
            p = params
        }
    }
}

