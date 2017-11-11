package com.github.aroq.drupipe

class DrupipeContainerBlock implements Serializable {

    String name

    ArrayList<DrupipeActionWrapper> actions = []

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipeAction execute - ${name}"
        for (actionWrapper in actions) {
            actionWrapper.pipeline = controller
            (new DrupipeActionWrapper(actionWrapper)).execute()
        }
    }

}
