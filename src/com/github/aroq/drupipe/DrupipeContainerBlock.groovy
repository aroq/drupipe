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
                configVersion: action.configVersion,
            ]
            action.remove('name')
            action.remove('methodName')
            action.remove('configVersion')

            actionWrapper['params'] = action
//            controller.utils.debugLog(controller.context, action, 'ACTION', [debugMode: 'json'], [], true)
            actionWrapper.pipeline = controller
            (new DrupipeActionWrapper(actionWrapper)).execute()
        }
    }

}
