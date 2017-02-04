def call(commandParams = [:], body) {
    commandParams << commandParams.actionParams['drupipeWithKubernetes'] << commandParams
    container(commandParams.containerName) {
        commandParams.workspace = pwd()
        sshagent([commandParams.credentialsID]) {
            result = body(commandParams)
            if (result) {
                commandParams << result
            }
        }
    }

    commandParams
}
