def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    utils = new com.github.aroq.workflowlibs.Utils()

    pipeline = utils.processPipeline(params.pipeline)
    jsonDump(pipeline)

    node {
        if (jenkinsParam('force') == '1') {
            deleteDir()
        }
        if (params.checkoutSCM) {
            checkout scm
        }

        stageConfig = [
            'config': [
                [
                    action: 'Config.perform',
                ],
            ]
        ]

        stage = utils.processStage(stageConfig)
        dump(stage, 'Stage object')
        params << executeStage(stage) {
            p = params
        }

        for (int i = 0; i < pipeline.size(); i++) {
            params.stage = pipeline[i]
            params << executeStage(pipeline[i]) {
                p = params
            }
        }
    }
    params
}

