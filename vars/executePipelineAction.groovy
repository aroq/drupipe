def call(action, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    if (params.p) {
        params << params.p
        params.remove('p')
    }

    params << new com.github.aroq.workflowlibs.Utils()

    result = executeAction(utils.processPipelineAction(action)) {
        p = params
    }

    params
}

