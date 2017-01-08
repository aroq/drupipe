def call(action, params = [:]) {
    utils = new com.github.aroq.drupipe.Utils()
    params << executeAction(utils.processPipelineAction(action)) {
        p = params
    }

    params
}

