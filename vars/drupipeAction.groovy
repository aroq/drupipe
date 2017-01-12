def call(action, context = [:]) {
    utils = new com.github.aroq.drupipe.Utils()
    context << context.pipeline.processPipelineAction(action, context).execute(context)
}

