#!groovy

def call(pipe, body = null) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                drupipeStages(pipe.stages, config)
            }
        }
    }
}
