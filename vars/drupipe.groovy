def call(commandParams = [:], body) {
    echo "CONFIG PARAMS: ${config}"
    timestamps {
        node('master') {
            configParams = executePipelineAction([action: 'Config.perform'], commandParams.clone() << params)
            commandParams << (configParams << commandParams)
        }
        result = body(commandParams)
        if (result) {
            commandParams << result
        }
        body(configParams)
    }
    configParams
}
