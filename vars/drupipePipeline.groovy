#!groovy

def call(pipe) {
    if (!pipe.params) {
        pipe.params = [:]
    }
    drupipe(pipe.params) { config ->
        config.block = [:]
        if (config.nodeName) {
            node(config.nodeName) {
                config.block.nodeName = config.nodeName
                if (config.drupipeDocker) {
                    drupipeWithDocker(config) {
                        pipe.blocks.each { block ->
                            drupipeStages(block.stages, config)
                        }
                    }
                }
                else {
                    pipe.blocks.each { block ->
                        drupipeStages(block.stages, config)
                    }
                }
            }
        }
        else {
            pipe.blocks.each { block ->
                drupipeStages(block.stages, config)
            }
        }
    }
}
