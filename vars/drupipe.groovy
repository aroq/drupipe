def call(commandParams = [:], body) {
    node('master') {
        configParams = executePipelineAction([action: 'Config.perform'], commandParams.clone() << params)
        commandParams << (configParams << commandParams)
    }
    result = body(commandParams)
    if (result) {
        commandParams << result
    }
    body(configParams)
    configParams
}
