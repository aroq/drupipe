def call(commandParams = [:], body) {
    commandParams << commandParams.actionParams['drupipeWithDocker'] << commandParams
    if (commandParams.dockerfile) {
        image = docker.build(commandParams.dockerfile, 'docroot/config')
    }
    else {
        image = docker.image(commandParams.drupipeDockerImageName)
        image.pull()
    }
    image.inside(commandParams.drupipeDockerArgs) {
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
