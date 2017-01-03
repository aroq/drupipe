#!groovy

def call(pipeline, body) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                drupipeStages(pipeline.stages)
            }
        }
    }
}
