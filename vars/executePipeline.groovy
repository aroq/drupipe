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

        stage 'config'
        configAction = [
            action: 'Config.perform',
            params: [
            ]
        ]
        params << executePipelineAction(configAction) {
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

