package com.github.aroq.drupipe

class DrupipeContainerBlock extends DrupipeBase {

    String name

    ArrayList<DrupipeActionWrapper> actions = []
    ArrayList<DrupipeActionWrapper> pre_actions = []
    ArrayList<DrupipeActionWrapper> post_actions = []

    ArrayList phases = ['pre_actions', 'actions', 'post_actions']

    DrupipeController controller

    def execute(body = null) {
        controller.drupipeLogger.trace "DrupipeContainerBlock execute - ${name}"

        for (phase in phases) {
            if (this."${phase}") {
                controller.drupipeLogger.debug "Execute CONTAINER BLOCK phase: ${phase}"
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
            controller.drupipeLogger.debugLog(controller.context, action, 'ACTION', [debugMode: 'json'])
            def actionWrapper = [
                name: action.name,
                methodName: action.methodName,
            ]
            action.remove('name')
            action.remove('methodName')

            controller.drupipeLogger.debugLog(controller.context, actionWrapper, 'ACTION WRAPPER', [debugMode: 'json'])
            DrupipeActionWrapper drupipeActionWrapper = new DrupipeActionWrapper(actionWrapper)
            drupipeActionWrapper.pipeline = controller
            drupipeActionWrapper.params = action
            drupipeActionWrapper.execute()
        }
    }

}
