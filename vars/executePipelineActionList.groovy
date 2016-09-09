def call(actions, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    if (params.p) {
        params << params.p
        params.remove('p')
    }

    utils = new com.github.aroq.workflowlibs.Utils()
    actionList = utils.processPipelineActionList(actions)
    debugLog(params, actionList, 'action list', [debugMode: 'json'])
    params << executeActionList(actionList) {
        p = params
    }
    params
}

