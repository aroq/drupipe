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

    node {
        stages = []
        stages << [
            [
                'config': [
                    [
                        action: 'Config.perform',
                    ],
                ],
            ]
        ]
        stages << params.pipeline
        dump(stages, 'Stages')
        pipeline = utils.processPipeline(stages)
        jsonDump(pipeline)
        if (jenkinsParam('force') == '1') {
            deleteDir()
        }
        if (params.checkoutSCM) {
            checkout scm
        }


        for (int i = 0; i < stages.size(); i++) {
            params.stage = stages[i]
            params << executeStage(stages[i]) {
                p = params
            }
        }
    }
    params
}

