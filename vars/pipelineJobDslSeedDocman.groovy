def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    executePipeline {
        checkoutSCM = true
        pipeline = [
            'init': [
                [
                    action: 'Docman.info',
                ],
                [
                    action: 'Source.add',
                    params: [
                        source: [
                              name: 'library',
                              type: 'git',
                              path: 'library',
                              url: 'https://github.com/aroq/drupipe.git',
                              branch: 'develop'
                        ]
                    ]
                ],
            ],
        ]
        p = params
    }
}
