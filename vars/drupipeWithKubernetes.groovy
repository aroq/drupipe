def call(context = [:], body) {
    context << context.actionParams['drupipeWithKubernetes'] << context
    container(context.containerName) {
        context.workspace = pwd()
        sshagent([context.credentialsID]) {
            drupipeAction(action: 'GitConfig.set', context)
            result = body(context)
            if (result) {
                context << result
            }
        }
    }

    context
}
