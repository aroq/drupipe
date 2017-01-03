#!groovy

def call(pipe, body) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                drupipeStages(pipe.stages)
            }
        }
    }
}

def call(pipe) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                drupipeStages(pipe.stages)
            }
        }
    }
}
