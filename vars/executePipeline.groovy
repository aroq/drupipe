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
        pipelineStages = [new com.github.aroq.workflowlibs.Stage(name: 'config', actionList: utils.processPipelineActionList([[action: 'Config.perform']]))]

        pipeline = utils.processPipeline(params.pipeline)
        pipelineStages += pipeline
        pipelineStages += utils.processStages(params.stages)

        jsonDump(pipelineStages)
        if (jenkinsParam('force') == '1') {
            deleteDir()
        }
        if (params.checkoutSCM) {
            checkout scm
        }

        for (int i = 0; i < pipelineStages.size(); i++) {
            params.stage = pipelineStages[i]
            params << executeStage(pipelineStages[i]) {
                p = params
            }
        }
    }
    params
}

