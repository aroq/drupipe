#!groovy

def call(stages, context) {
    context.pipeline.executeStages(stages, context)
}

