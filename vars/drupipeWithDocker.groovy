def call(context = [:], body) {
    context << context.actionParams['drupipeWithDocker'] << context
    if (context.dockerfile) {
        image = docker.build(context.dockerfile, 'docroot/config')
    }
    else {
        image = docker.image(context.dockerImage)
        image.pull()
    }
    image.inside(context.drupipeDockerArgs) {
        context.workspace = pwd()
        sshagent([context.credentialsID]) {
            result = body(context)
            if (result) {
                context << result
            }
        }
    }

    context
}
