#!groovy

def call(blockParams = [:], config, body) {
    def result = [:]
    echo "BLOCK PARAMS: ${blockParams}"
    echo "BLOCK CONFIG: ${config}"
    if (blockParams.nodeName) {
        config.nodeName = blockParams.nodeName
        node(config.nodeName) {
            if (blockParams.drupipeDocker) {
                config.drupipeDocker = blockParams.drupipeDocker
                withDrupipeDocker(config) {
                    result = body()
                }
            }
            else {
                config.drupipeDocker = null
                result = body()
            }
        }
    }
    else {
        config.nodeName = null
        config.drupipeDocker = null
        result = body()
    }
    result
}
