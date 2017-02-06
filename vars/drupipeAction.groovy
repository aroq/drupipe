def call(action, context = [:]) {
    script.echo "defaultParams drupipeAction: ${params.defaultParams}"
    (context.pipeline.processPipelineAction(action, context)).execute()
}

