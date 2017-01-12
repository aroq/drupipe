package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    ArrayList<DrupipeBlock> blocks = []

    LinkedHashMap params = [:]

    def script

    def execute() {
        script.drupipe(this.params) { context ->
            context.block = [:]
            if (context.nodeName) {
                script.node(context.nodeName) {
                    context.block.nodeName = context.nodeName
                    if (context.drupipeDocker) {
                        script.drupipeWithDocker(context) {
                            blocks.each { block ->
                                script.drupipeStages(block.stages, context)
                            }
                        }
                    }
                    else {
                        blocks.each { block ->
                            script.drupipeStages(block.stages, context)
                        }
                    }
                }
            }
            else {
                blocks.each { block ->
                    script.drupipeStages(block.stages, context)
                }
            }
        }
    }

}
