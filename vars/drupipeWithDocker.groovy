def call(context = [:], body) {
    context << context.defaultActionParams['drupipeWithDocker'] << context
    if (context.dockerfile) {
        image = docker.build(context.dockerfile, 'docroot/config')
    }
    else {
        image = docker.image(context.dockerImage)
        image.pull()
    }
    image.inside(context.drupipeDockerArgs) {
        context.workspace = pwd()
        result = body(context)
        if (result) {
            context << result
        }
    }

    context
}
