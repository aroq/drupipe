package com.github.aroq.drupipe

class DrupipeContainerBlock extends DrupipeBase {

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
                    executeAction(processAction(action))
                }
            }
        }
    }

    def processAction(action) {
        controller.drupipeConfig.processItem(action, 'actions', 'params', 'execute')
    }

    def executeAction(action) {
        if (action) {
            controller.utils.debugLog(controller.context, action, 'ACTION', [debugMode: 'json'], [], false)
            def actionWrapper = [
                name: action.name,
                methodName: action.methodName,
//                configVersion: action.configVersion,
            ]
            action.remove('name')
            action.remove('methodName')
//            action.remove('configVersion')

            controller.utils.debugLog(controller.context, actionWrapper, 'ACTION WRAPPER', [debugMode: 'json'], [], false)
            DrupipeActionWrapper drupipeActionWrapper = new DrupipeActionWrapper(actionWrapper)
            drupipeActionWrapper.pipeline = controller
            drupipeActionWrapper.params = action
            drupipeActionWrapper.execute()
        }
    }

}
