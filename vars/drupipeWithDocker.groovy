def call(context = [:], body) {

    context << context.parameters.action['drupipeWithDocker'] << context
    if (context.dockerfile) {
        image = docker.build(context.dockerfile, context.projectConfigPath)
    }
    else {
        image = docker.image(context.dockerImage)
        image.pull()
    }
    def drupipeDockerArgs = context.drupipeDockerArgs
    image.inside(drupipeDockerArgs) {
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
