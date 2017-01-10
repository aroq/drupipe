#!groovy

def call(blockParams = [:], config, body) {
    def result = [:]
    config.block = [:]
    echo "BLOCK PARAMS: ${blockParams}"
    echo "BLOCK CONFIG: ${config}"
    if (blockParams.nodeName) {
        config.block.nodeName = blockParams.nodeName
        node(config.nodeName) {
            if (blockParams.drupipeDocker) {
                config.block.drupipeDocker = blockParams.drupipeDocker
                withDrupipeDocker(config) {
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
