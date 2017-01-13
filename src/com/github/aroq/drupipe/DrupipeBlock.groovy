package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    LinkedHashMap blockParams = [:]

    LinkedHashMap context = [:]

    def execute(context, body = null) {
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
