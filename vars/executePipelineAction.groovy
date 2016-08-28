def call(action, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    if (params.p) {
        params << params.p
        params.remove('p')
    }

    params << executeAction(utils.processPipelineAction(action)) {
        p = params
    }

    params
}

