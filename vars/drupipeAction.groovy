def call(action, context = [:]) {
    echo "defaultParams drupipeAction: ${action.params.defaultParams}"
    (context.pipeline.processPipelineAction(action, context)).execute()
}

