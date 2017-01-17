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
                        script: params.deployScript,
                        args: [
                            '@dev.default',
                            //jenkinsParam('alias')
                        ]
                    ]
                ]
            ],
            'test': [
                [
                    action: 'Script.execute',
                    params: [
                        script: params.testScript,
                        args: [
                            '@dev.default',
                            //jenkinsParam('alias')
                        ]
                    ]
                ]
            ]
        ]
    }

}
