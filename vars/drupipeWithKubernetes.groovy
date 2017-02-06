def call(context = [:], body) {
    context << context.actionParams['drupipeWithKubernetes'] << context
    container(context.containerName) {
        drupipeAction(action: 'GitConfig.set', context)
        context.workspace = pwd()
        sshagent([context.credentialsID]) {
            result = body(context)
            if (result) {
                context << result
            }
        }
    }

    context
}
