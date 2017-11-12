package com.github.aroq.drupipe

class DrupipeContainer implements Serializable {

    String name

    String image

    ArrayList<DrupipeBlock> blocks = []

    DrupipeController controller

    def execute(body = null) {
        controller.script.echo "DrupipeContainer execute - ${name}"

        for (block in blocks) {
            controller.utils.debugLog(controller.context, block, 'CONTAINER BLOCK', [debugMode: 'json'], [], true)
            block = new DrupipeContainerBlock(block)
            block.controller = controller
            block.execute()
        }

    }

}
