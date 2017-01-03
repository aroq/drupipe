#!groovy

def call(LinkedHashMap pipe) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                drupipeStages(pipe.stages)
            }
        }
    }
}
