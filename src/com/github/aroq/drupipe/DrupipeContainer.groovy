package com.github.aroq.drupipe

class DrupipeContainer implements Serializable {

    String name

    String image

    ArrayList<DrupipeBlock> blocks = []

    DrupipeController controller

    def execute(body = null) {
        controller.script.echo "DrupipeContainer execute - ${name}"

        for (block in blocks) {
            block = new DrupipeContainerBlock(block)
            block.controller = controller
            block.execute()
        }

    }

}
