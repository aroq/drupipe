#!groovy

def call(blockParams, config, body) {
    if (blockParams.nodeName) {
        config.nodeName = blockParams.nodeName
        node(config.nodeName) {
            if (blockParams.drupipeDocker) {
                config.drupipeDocker = blockParams.drupipeDocker
                withDrupipeDocker(config) {
                    result = body(config)
                }
            }
            else {
                config.drupipeDocker = null
                result = body(config)
            }
        }
    }
    else {
        config.nodeName = null
        config.drupipeDocker = null
        result = body(config)
    }
}
