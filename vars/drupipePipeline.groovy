#!groovy

def call(pipe, body = null) {
    if (!pipe.params) {
        pipe.params = [:]
    }
    drupipe(pipe.params) { config ->
        if (config.nodeName) {
            node(config.nodeName) {
                config.block.nodeName = config.nodeName
                if (config.drupipeDocker) {
                    withDrupipeDocker(config) {
                        drupipeStages(pipe.stages, config)
                    }
                }
                else {
                    drupipeStages(pipe.stages, config)
                }
            }
        }
        else {
            drupipeStages(pipe.stages, config)
        }
    }
}
