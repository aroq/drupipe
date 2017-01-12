package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    ArrayList<DrupipeBlock> blocks = []

    LinkedHashMap params = [:]

    def execute() {
        drupipe(this.params) { context ->
            context.block = [:]
            if (context.nodeName) {
                node(context.nodeName) {
                    context.block.nodeName = context.nodeName
                    if (context.drupipeDocker) {
                        drupipeWithDocker(context) {
                            blocks.each { block ->
                                drupipeStages(block.stages, context)
                            }
                        }
                    }
                    else {
                        blocks.each { block ->
                            drupipeStages(block.stages, context)
                        }
                    }
                }
            }
            else {
                blocks.each { block ->
                    drupipeStages(block.stages, context)
                }
            }
        }
    }

}
