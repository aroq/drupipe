def call(context = [:], body) {
    context << context.defaultActionParams['drupipeWithDocker'] << context
    if (context.dockerfile) {
        image = docker.build(context.dockerfile, context.projectConfigPath)
    }
    else {
        image = docker.image(context.dockerImage)
        image.pull()
    }
    image.inside(context.drupipeDockerArgs) {
        context.workspace = pwd()
        sshagent([context.credentialsId]) {
            result = body(context)
            if (result) {
                context << result
            }
        }
    }

    context
}
