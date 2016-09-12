def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [[$class: 'StringParameterDefinition', defaultValue: '', description: 'Some Description', name : 'MY_PARAM'], [$class: 'StringParameterDefinition', defaultValue: '', description: 'Some Description', name: 'MY_PARAM2']]]])
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
        dump(params, 'pipeline result')
        params
    }
}
