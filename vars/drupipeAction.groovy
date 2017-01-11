def call(action, params = [:]) {
    utils = new com.github.aroq.drupipe.Utils()
    params << utils.processPipelineAction(action).execute()

    params
}

