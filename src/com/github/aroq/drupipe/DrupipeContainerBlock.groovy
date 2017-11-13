package com.github.aroq.drupipe

class DrupipeContainerBlock implements Serializable {

    String name

    ArrayList<DrupipeActionWrapper> actions = []
    ArrayList<DrupipeActionWrapper> pre_actions = []
    ArrayList<DrupipeActionWrapper> post_actions = []

    ArrayList phases = ['pre_actions', 'actions', 'post_actions']

    DrupipeController controller

    def script = controller.script

    def execute(body = null) {
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

            actionWrapper['params'] = action
            controller.utils.debugLog(controller.context, action, 'ACTION', [debugMode: 'json'], [], true)
            actionWrapper.pipeline = controller
            (new DrupipeActionWrapper(actionWrapper)).execute()
        }
    }

}
