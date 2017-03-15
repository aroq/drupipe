def call(context = [:], body) {
    context << context.defaultActionParams['drupipeWithKubernetes'] << context
    container(context.containerName) {
        context.workspace = pwd()
        sshagent([context.credentialsId]) {
            drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", context)
            result = body(context)
            if (result) {
                context << result
            }
        }
    }

    context
}
