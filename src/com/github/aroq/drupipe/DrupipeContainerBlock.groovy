package com.github.aroq.drupipe

class DrupipeContainerBlock implements Serializable {

    String name

    ArrayList<DrupipeActionWrapper> actions = []

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipeContainerBlock execute - ${name}"
        for (action in actions) {
            def actionWrapper = [
                name: action.name,
                methodName: action.methodName,
            ]
            actionWrapper.pipeline = controller
            (new DrupipeActionWrapper(actionWrapper)).execute()
        }
    }

}
