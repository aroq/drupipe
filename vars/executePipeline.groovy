def call(name, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    params << params.p
    params.remove('p')

    pipeline = pipelineParse(params.pipeline)

    for (int i = 0; i < pipeline.size(); i++) {
        params.stage = pipeline[i]
        params << executeStage(pipeline[i].name) {
            p = params
        }
    }
}

@NonCPS
def pipelineParse(pipeline) {
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
