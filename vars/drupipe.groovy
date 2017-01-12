def call(commandParams = [:], body) {
    timestamps {
        if (!commandParams['Config_perform']) {
            node('master') {
                configParams = drupipeAction([action: 'Config.perform'], commandParams.clone() << params)
                commandParams << (configParams << commandParams)
            }
        }

        if (params.force == '11') {
            echo 'FORCE REMOVE DIR'
            deleteDir()
        }
        if (params.checkoutSCM) {
            checkout scm
        }

        result = body(commandParams)

        if (result) {
            commandParams << result
        }
    }

    configParams
}
