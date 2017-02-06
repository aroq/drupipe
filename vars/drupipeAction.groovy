def call(action, context = [:]) {
    echo "defaultParams drupipeAction: ${params.defaultParams}"
    (context.pipeline.processPipelineAction(action, context)).execute()
}

