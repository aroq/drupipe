def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        // Set environment based on branch name.
        def environment = 'local'
        switch (env.BRANCH_NAME) {
            case 'develop':
                environment = 'dev'
                break
            case 'master':
                environment = 'test'
                break

        }
        properties(
            [
                [$class: 'GitLabConnectionProperty', gitLabConnection: 'Gitlab'],
                [
                    $class: 'ParametersDefinitionProperty',
                    parameterDefinitions: [
                        [
                            name : 'force',
                            $class: 'StringParameterDefinition',
                            defaultValue: '0',
                            description: 'Force mode',
                        ],
                        [
                            name : 'debug',
                            $class: 'StringParameterDefinition',
                            defaultValue: '0',
                            description: 'Debug mode',
                        ],
                        [
                            name : 'alias',
                            $class: 'StringParameterDefinition',
                            defaultValue: "@${environment}.default",
                            description: 'Force mode',
                        ],
                        [$class: 'StringParameterDefinition', defaultValue: '', description: '', name: 'payload']
                    ]
                ]
            ]
        )

        echo ("This build is built with the payload: $payload")

        result = executePipeline {
            checkoutSCM = true
            pipeline = [
                'init': [
                    [
                        action: 'Source.add',
                        params: [
                            source: [
                                name: 'root',
                                type: 'dir',
                                path: './',
                            ]
                        ]
                    ],
                    [
                        action: 'Source.loadConfig',
                        params: [
                            sourceName: 'root',
                            configType: 'groovy',
                        ]
                    ]
                ],
            ]
            p = params
        }
    }
}
