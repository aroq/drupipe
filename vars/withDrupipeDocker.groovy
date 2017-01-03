def call(commandParams = [:], body) {
    node(commandParams.nodeName) {
        timestamps {
            commandParams << commandParams.actionParams['withDrupipeDocker'] << commandParams
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

    commandParams
}
