def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    pipelineMultibranchBase {
        p = params
        stages = [
            'ops': [
                [
                    action: 'Script.execute',
                    params: [
                        script: params.script,
                        args: [
                            '@dev.default',
                            jenkinsParam('alias')
                        ]
                    ]
                ]
            ]
        ]
    }

}
