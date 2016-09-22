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
        pipelinestages = [new com.github.aroq.workflowlibs.Stage(name: 'config', actionList: utils.processPipelineActionList([[action: 'Config.perform']]))]

        pipeline = utils.processPipeline(params.pipeline)
        pipelinestages += pipeline
        pipelinestages += utils.processStages(params.pipelinestages)

        jsonDump(pipelinestages)
        if (jenkinsParam('force') == '1') {
            deleteDir()
        }
        if (params.checkoutSCM) {
            checkout scm
        }

        for (int i = 0; i < pipelinestages.size(); i++) {
            jsonDump(pipelinestages[i])
//            params.stage = pipelinestages[i]
//            params << executeStage(pipelinestages[i]) {
//                p = params
//            }
        }
    }
    params
}

