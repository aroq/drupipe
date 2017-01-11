#!groovy

def call(blockParams = [:], config, body) {
    def result = [:]
    config.block = [:]
    if (blockParams.nodeName) {
        config.block.nodeName = blockParams.nodeName
        node(config.block.nodeName) {
            if (blockParams.drupipeDocker) {
                config.block.drupipeDocker = blockParams.drupipeDocker
                drupipeWithDocker(config) {
                    result = body()
                }
            }
            else {
                config.block.drupipeDocker = null
                result = body()
            }
        }
    }
    else {
        config.block.nodeName = null
        config.block.drupipeDocker = null
        result = body()
    }
    result
}
