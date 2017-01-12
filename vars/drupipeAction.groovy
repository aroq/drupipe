def call(action, context = [:]) {
    context << (context.pipeline.processPipelineAction(action, context)).execute(context)
}

