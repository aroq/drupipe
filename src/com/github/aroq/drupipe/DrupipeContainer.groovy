package com.github.aroq.drupipe

class DrupipeContainer implements Serializable {

    String name

    String image

    ArrayList<DrupipeBlock> blocks = []

    DrupipeController controller

    DrupipePod pod

    def execute(body = null) {
        controller.script.echo "DrupipeContainer execute - ${name}"

        if (pod.containerized && controller.context.containerMode != 'kubernetes') {
            if (controller.context.dockerfile) {
                image = controller.script.docker.build(controller.context.dockerfile, controller.context.projectConfigPath)
            }
            else {
                image = controller.script.docker.image(controller.context.dockerImage)
                controller.script.image.pull()
            }
            def drupipeDockerArgs = controller.context.drupipeDockerArgs
            controller.script.image.inside(drupipeDockerArgs) {
                controller.context.workspace = controller.script.pwd()
                controller.script.sshagent([controller.context.credentialsId]) {
                    if (body) {
                        body(controller.context)
                        executeBlocks()
                    }

                }
            }

        }
        else {
            executeBlocks()
        }
    }

    def executeBlocks() {
        for (block in blocks) {
            controller.utils.debugLog(controller.context, block, 'CONTAINER BLOCK', [debugMode: 'json'], [], true)
            block = new DrupipeContainerBlock(block)
            block.controller = controller
            block.execute()
        }
    }

}
