def call(context = [:], blockParams = [:], body) {
    context << context.defaultActionParams['drupipeWithDocker'] << context
    if (context.dockerfile) {
        image = docker.build(context.dockerfile, context.projectConfigPath)
    }
    else {
        image = docker.image(context.dockerImage)
        image.pull()
    }
    def drupipeDockerArgs = context.drupipeDockerArgs
    if (blockParams.workingDir) {
        drupipeDockerArgs += " --working-dir=${blockParams.workingDir}"
    }
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
