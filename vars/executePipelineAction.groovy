def call(action, params = [:]) {
    utils = new com.github.aroq.workflowlibs.Utils()
    params << executeAction(utils.processPipelineAction(action)) {
        p = params
    }

    params
}

