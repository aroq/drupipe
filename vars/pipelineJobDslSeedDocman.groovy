def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    if (params.params) {
        params << params.params
        params.remove('params')
    }

    utils = new com.github.aroq.drupipe.Utils()

    def drupipePipeline =
        [
            'init': [
                [
                    action: 'Docman.info',
                ],
            ],
        ]

    utils.executeStages([
        pipeline: drupipePipeline
    ])
}
