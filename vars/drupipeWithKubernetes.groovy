def call(context = [:], body) {
    nodeName = 'drupipe'
    containerTemplate = 'block'

    podTemplate(label: nodeName, containers: [
        containerTemplate(name: 'block', image: "michaeltigr/zebra-build-php-drush-docman", ttyEnabled: true, command: 'cat'),
    ]) {
        node(nodeName) {
            container(containerTemplate) {
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
