def call(commandParams = [:], body) {
    timestamps {
        if (!commandParams['Config_perform']) {
            node('master') {
                configParams = executePipelineAction([action: 'Config.perform'], commandParams.clone() << params)
                commandParams << (configParams << commandParams)
            }
        }
        result = body(commandParams)
        if (result) {
            commandParams << result
        }
    }

    configParams
}
