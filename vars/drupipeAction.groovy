def call(action, params = [:]) {
    utils = new com.github.aroq.drupipe.Utils()
    params << utils.executeAction(utils.processPipelineAction(action), params)

    params
}

