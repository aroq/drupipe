def call(action, context = [:]) {
    (context.pipeline.processPipelineAction(action)).execute(context)
}

