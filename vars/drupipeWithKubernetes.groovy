def call(context = [:], body) {

    nodeName = 'drupipe'

    podTemplate(label: nodeName, inheritFrom: 'default',  containers: [
        containerTemplate(name: context.containerName, image: context.dockerImage)
    ]) {
        context.pipeline.script.echo "NODE NAME: ${nodeName}"
        context.pipeline.script.node(nodeName) {
            context.pipeline.script.unstash('config')
            context << context.defaultActionParams['drupipeWithKubernetes'] << context
            container(context.containerName) {
                context.workspace = pwd()
                drupipeShell("echo 'test'", context)
                sshagent([context.credentialsId]) {
//            drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", context)
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
