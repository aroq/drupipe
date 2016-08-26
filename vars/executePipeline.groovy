def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    pipeline = processPipeline(params.pipeline)

    node {
        if (params.checkoutSCM) {
            checkout scm
        }
        tempParams = params
        for (int i = 0; i < pipeline.size(); i++) {
            params.stage = pipeline[i]
            params << executeStage(pipeline[i].name) {
                p = params
            }
        }
    }
}

@NonCPS
def processPipeline(pipeline) {
    List<com.github.aroq.workflowlibs.Stage> result = []
    for (item in pipeline) {
        List<String> actions = []
        for (action in item.value) {
            actions << action
        }
        result << new com.github.aroq.workflowlibs.Stage(name: item.key, actionList: actions)
    }
    result
}
