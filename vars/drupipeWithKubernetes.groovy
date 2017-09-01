def call(context = [:], body) {
    nodeName = 'drupipe'
    containerName = 'drupipe-container'

    podTemplate(label: nodeName, containers: [
        containerTemplate(name: containerName, image: context.dockerImage, ttyEnabled: true, command: 'cat'),
    ]) {
        node(nodeName) {
            container(containerName) {
                unstash('config')
                context << context.defaultActionParams['drupipeWithKubernetes'] << context
                context.workspace = pwd()
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
