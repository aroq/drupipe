package com.github.aroq.drupipe

class DrupipeJob implements Serializable {

    String name

    DrupipePipeline pipeline

    DrupipeController controller

    def init() {

    }

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipeJob execute - ${name}"
    }

}
