def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    if (params.params) {
        params << params.params
        params.remove('params')
    }

    def drupipePipeline =
        [
            'init': [
                [
                    action: 'Docman.info',
                ],
            ],
        ]

    executeStages([
        pipeline: drupipePipeline
    ])
}
