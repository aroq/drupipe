def call(action, context = [:]) {
    (action.pipeline.processPipelineAction(action, context)).execute()
}

