package com.github.aroq.drupipe

class DrupipeContainer implements Serializable {

    String name

    ArrayList<DrupipeBlock> blocks = []

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipeContainer execute - ${name}"
    }

}
