package com.github.aroq.drupipe

class DrupipeContainerBlock implements Serializable {

    String name

    ArrayList<DrupipeActionWrapper> actions = []
    ArrayList<DrupipeActionWrapper> pre_actions = []
    ArrayList<DrupipeActionWrapper> post_actions = []

    ArrayList phases = ['pre_actions', 'actions', 'post_actions']

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script

        script.echo "DrupipeContainerBlock execute - ${name}"

        for (phase in phases) {
            if (this."${phase}") {
                script.echo "Execute CONTAINER BLOCK phase: ${phase}"
                for (action in this."${phase}") {
                    executeAction(action)
                }
            }
        }
    }

    def executeAction(action) {
        if (action) {
            def actionWrapper = [
                name: action.name,
                methodName: action.methodName,
                configVersion: action.configVersion,
            ]
            action.remove('name')
            action.remove('methodName')
            action.remove('configVersion')

            controller.utils.debugLog(controller.context, action, 'ACTION', [debugMode: 'json'], [], true)
            DrupipeActionWrapper drupipeActionWrapper = new DrupipeActionWrapper(actionWrapper)
            drupipeActionWrapper.pipeline = controller
            drupipeActionWrapper.params = action
            if (controller.context.results) {
                controller.script.echo "DrupipeContainerBlock.executeAction(): serializeAndDeserialize(pipeline.context.results) BEFORE0"
                controller.utils.serializeAndDeserialize(controller.context.results)
                controller.script.echo "DrupipeContainerBlock.executeAction(): serializeAndDeserialize(pipeline.context.results) AFTER0"
            }
            drupipeActionWrapper.execute()
            if (controller.context.results) {
                controller.script.echo "DrupipeContainerBlock.executeAction(): serializeAndDeserialize(pipeline.context.results) BEFORE1"
                controller.utils.serializeAndDeserialize(controller.context.results)
                controller.script.echo "DrupipeContainerBlock.executeAction(): serializeAndDeserialize(pipeline.context.results) AFTER1"
            }
        }
    }

}
