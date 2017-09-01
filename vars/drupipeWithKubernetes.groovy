def call(context = [:], body) {
    podTemplate(label: 'mypod', containers: [
        containerTemplate(name: context.containerName, image: context.dockerImage, ttyEnabled: true, command: 'cat'),
    ]) {
        node('mypod') {
            container(context.containerName) {
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
