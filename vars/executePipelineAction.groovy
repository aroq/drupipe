def call(action, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    jsonDump(params.p, 'params.p')

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    utils = new com.github.aroq.workflowlibs.Utils()
    params << executeAction(utils.processPipelineAction(action)) {
        p = params
    }

    params
}

