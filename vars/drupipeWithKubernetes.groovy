def call(context = [:], body) {
    podTemplate(label: 'mypod', containers: [
        containerTemplate(name: 'docman', image: context.dockerImage, ttyEnabled: true, command: 'cat'),
    ]) {
        node('mypod') {
            container('docman') {
                unstash('config')
                context << context.defaultActionParams['drupipeWithKubernetes'] << context
                context.workspace = pwd()
                sshagent([context.credentialsId]) {
//                    drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", context)
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
