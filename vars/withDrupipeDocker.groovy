def call(commandParams = [:], body) {
    node(commandParams.nodeName) {
        timestamps {
            timeout(20) {
                configParams = executePipelineAction([action: 'Config.perform'], commandParams.clone() << params)
                commandParams << ((configParams << configParams.actionParams['withDrupipeDocker']) << commandParams)
                if (commandParams.dockerfile) {
                    image = docker.build(commandParams.dockerfile, 'docroot/config')
                }
                else {
                    image = docker.image(commandParams.drupipeDockerImageName)
                    image.pull()
                }
                image.inside(commandParams.drupipeDockerArgs) {
                    sshagent([commandParams.credentialsID]) {
                        result = body(commandParams)
                        if (result) {
                            commandParams << result
                        }
                    }
                }
            }
        }
    }

    commandParams
}
