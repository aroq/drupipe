import com.github.aroq.drupipe.DrupipeController

def call(DrupipeController pipeline, body) {
    String nodeName = 'default'
    String containerName = 'drupipecontainer'

    podTemplate(label: nodeName, containers: [
        containerTemplate(name: containerName, image: pipeline.context.dockerImage, ttyEnabled: true, command: 'cat', alwaysPullImage: true),
    ]) {
        node(nodeName) {
            container(containerName) {
                unstash('config')
                pipeline.context.workspace = pwd()
                pipeline.scmCheckout()
                sshagent([pipeline.context.credentialsId]) {
                    result = body(pipeline.context)
                }
            }
        }
    }
}