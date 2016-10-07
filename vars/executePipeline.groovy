def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }
    echo "TEST"
    sh('ls -al')

    utils = new com.github.aroq.workflowlibs.Utils()
    if (params.noNode) {
        params << _executePipeline(params)
    }
    else {
        node {
            params << _executePipeline(params)
        }
    }

    params
}

def _executePipeline(params) {
    stages = [new com.github.aroq.workflowlibs.Stage(name: 'config', actionList: utils.processPipelineActionList([[action: 'Config.perform']]))]

    stages += utils.processPipeline(params.pipeline)
    stages += utils.processStages(params.stages)

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
    params
}

