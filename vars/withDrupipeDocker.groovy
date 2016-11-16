def call(commandParams = [:], body) {
    node(commandParams.nodeName) {
        configParams = executePipelineAction([action: 'Config.perform'], commandParams.clone() << params)
        commandParams << ((configParams << configParams.actionParams['withDrupipeDocker']) << commandParams)

        def image = docker.image(commandParams.drupipeDockerImageName)
        image.pull()
        image.inside(commandParams.drupipeDockerArgs) {
            sshagent([commandParams.credentialsID]) {
                result = body(commandParams)
                if (result) {
                    commandParams << result
                }
            }
        }
    }

    commandParams
}
