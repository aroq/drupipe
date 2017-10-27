def call(action, pipeline = null) {
    def pipe = pipeline ? pipeline : action.pipeline
    (pipe.processPipelineAction(action)).execute()
}

