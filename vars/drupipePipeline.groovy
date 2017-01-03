#!groovy

def call(pipe) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                drupipeStages(pipe.stages)
            }
        }
    }
}
