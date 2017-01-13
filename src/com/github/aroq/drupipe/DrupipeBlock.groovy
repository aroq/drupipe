package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String nodeName = null

    Boolean drupipeDocker = false

    LinkedHashMap context = [:]

    def execute(context, body = null) {
        if (context) {
            this.context = context
        }

        def result = [:]
        context.block = this

        if (this.nodeName) {
            context.pipeline.script.node(context.nodeName) {
                if (context.drupipeDocker) {
                    context.pipeline.script.drupipeWithDocker(context) {
                        result = _execute(body)
                    }
                }
                else {
                    result = _execute(body)
                }
            }
        }
        else {
            result = _execute(body)
        }

        result
    }

    def _execute(body = null) {
        if (stages) {
            context << context.pipeline.executeStages(stages, context)
        }
        else {
            context << body()
        }
        context
    }
}
