#!groovy

def call(pipe, body = null) {
    drupipe() { config ->
        node(config.nodeName) {
            echo "CONFIG PARAMS:${config}"
            withDrupipeDocker(config) {
                drupipeStages(pipe.stages, config)
            }
        }
    }
}
