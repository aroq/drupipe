#!groovy

// Pipeline used to create project specific pipelines.
def call(LinkedHashMap p = [:]) {
    drupipe { pipeline ->
        drupipeBlock(nodeName: 'master', pipeline) {
            drupipeAction([action: 'JobDslSeed.prepare'], pipeline)
            drupipeAction([action: 'JobDslSeed.perform'], pipeline)
        }
    }
}
