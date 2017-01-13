package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String nodeName = ''

    Boolean drupipeDocker = false

    LinkedHashMap context = [:]

    def execute(context, body = null) {
        if (context) {
            this.context = context
        }

        def result = [:]
        context.block = [:]

        if (this.nodeName) {
            context.block.nodeName = this.nodeName
            context.pipeline.script.node(context.nodeName) {
                context.block.nodeName = this.nodeName
                if (context.drupipeDocker) {
                    context.block.drupipeDocker = this.drupipeDocker
                    context.pipeline.script.drupipeWithDocker(context) {
                        result = _execute(body)
                    }
                }
                else {
                    context.block.drupipeDocker = null
                    result = _execute(body)
                }
            }
        }
        else {
            context.block.nodeName = null
            context.block.drupipeDocker = null
            result = _execute(body)
        }

        result
    }

    def _execute(body = null) {
        if (blocks) {
            blocks.each { block ->
                context << script.drupipeStages(block.stages, context)
            }
        }
        else {
            context << body()
        }
        context
    }
}
