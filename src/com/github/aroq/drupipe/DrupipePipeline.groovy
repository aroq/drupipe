package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    String name

    ArrayList<DrupipePod> pods = []

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipePipeline execute - ${name}"
    }

}
