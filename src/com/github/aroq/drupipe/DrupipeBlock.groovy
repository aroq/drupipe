package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    LinkedHashMap blockParams = [:]

    LinkedHashMap context = [:]

    def execute(context, body = null) {
        if (context) {
            this.context = context
        }

        def result = [:]
        context.block = [:]
        context.pipeline.script.echo "CONTEXT: ${context}" {

//        if (blockParams.nodeName) {
//            context.block.nodeName = blockParams.nodeName
//            context.pipeline.node(context.block.nodeName) {
//                if (blockParams.drupipeDocker) {
//                    context.block.drupipeDocker = blockParams.drupipeDocker
//                    context.pipeline.drupipeWithDocker(context) {
//                        result = body()
//                    }
//                }
//                else {
//                    result = body()
//                }
//            }
//        }
//        else {
//            result = body()
//        }

        if (blockParams.nodeName) {
            context.block.nodeName = blockParams.nodeName
            context.pipeline.script.node(context.nodeName) {
                context.block.nodeName = blockParams.nodeName
                if (context.drupipeDocker) {
                    context.block.drupipeDocker = blockParams.drupipeDocker
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
