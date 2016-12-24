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
                [
                    action: 'Source.add',
                    params: [
                        source: [
                              name: 'library',
                              type: 'git',
                              path: 'library',
                              url: params.drupipeLibraryUrl,
                              branch: params.drupipeLibraryBranch,
                        ],
                    ],
                ],
            ],
        ]

    executeStages([
        pipeline: drupipePipeline
    ])
}
