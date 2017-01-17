package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String nodeName = 'config.nodeName'

    Boolean drupipeDocker = false

    LinkedHashMap context = [:]

    def execute(c, body = null) {
        if (c) {
            this.context = c
        }

        if (this.nodeName == 'config.nodeName') {
            this.nodeName = context.nodeName
        }

        def result = [:]
        context.block = this

        if (this.nodeName) {
            context.pipeline.script.node(this.nodeName) {
                if (context.block.drupipeDocker) {
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
