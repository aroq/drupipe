def call(action, context = [:]) {
    (context.pipeline.processPipelineAction(action, context)).execute()
}

