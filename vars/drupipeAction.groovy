def call(action, context = [:], pipeline = null) {
    def pipe = pipeline ? pipeline : action.pipeline
    (pipe.processPipelineAction(action, context)).execute()
}

