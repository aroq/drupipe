package com.github.aroq.drupipe

class DrupipeContainer extends DrupipeBase {

    String name

    String image

    ArrayList blocks = []

    DrupipeController controller

    DrupipePod pod

    LinkedHashMap k8s

    def execute(body = null) {
        controller.script.echo "DrupipeContainer execute - ${name}"

        def dockerImage

        if (pod.containerized && controller.context.containerMode != 'kubernetes') {
            if (controller.context.dockerfile) {
                dockerImage = controller.script.docker.build(controller.context.dockerfile, controller.context.projectConfigPath)
            }
            else {
                dockerImage = controller.script.docker.image(this.image)
                dockerImage.pull()
            }
            def drupipeDockerArgs = controller.context.drupipeDockerArgs
            dockerImage.inside(drupipeDockerArgs) {
                controller.context.workspace = controller.script.pwd()
                controller.script.sshagent([controller.context.credentialsId]) {
                    if (body) {
                        body(controller.context)
                    }
                    executeBlocks()
                }
            }
        }
        else {
            executeBlocks()
        }
    }

    def executeBlocks() {
        for (block in blocks) {
//            controller.controller.drupipeLogger.debugLog(controller.context, block, 'CONTAINER BLOCK', [debugMode: 'json'], [], true)
            block = new DrupipeContainerBlock(block)
            block.controller = controller
            block.execute()
        }
    }

}
