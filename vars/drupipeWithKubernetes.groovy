def call(context = [:], body) {
    nodeName = 'drupipe'
    containerName = 'drupipecontainer'

    podTemplate(label: nodeName, containers: [
        containerTemplate(name: containerName, image: context.dockerImage, ttyEnabled: true, command: 'cat', alwaysPullImage: true),
    ]) {
        node(nodeName) {
            container(containerName) {
                unstash('config')
                context << context.defaultActionParams['drupipeWithKubernetes'] << context
                context.workspace = pwd()
                context.pipeline.scmCheckout()
                sshagent([context.credentialsId]) {
                    result = body(context)
                    if (result) {
                        context << result
                    }
                }
            }
        }
    }

    context
}
