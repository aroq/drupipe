package com.github.aroq.drupipe

class DrupipeContainer implements Serializable {

    String name

    String image

    ArrayList<DrupipeBlock> blocks = []

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipeContainer execute - ${name}"

        for (block in blocks) {
            block = DrupipeBlock(block)
            block.pipeline = controller
            block.execute()
        }

    }

}
