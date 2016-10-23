def call(action, params) {
    jsonDump(params, 'params')

    utils = new com.github.aroq.workflowlibs.Utils()
    params << executeAction(utils.processPipelineAction(action)) {
        p = params
    }

    params
}

