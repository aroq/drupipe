package com.github.aroq.drupipe

class DrupipePod implements Serializable {

    String name

    ArrayList<DrupipeContainer> containers = []

    DrupipeController controller

    boolean containerized = true

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipePod execute - ${name}"

        script.node(name) {
            for (container in containers) {
//                controller.utils.debugLog(controller.context, container, 'CONTAINER', [debugMode: 'json'], [], true)
                container = new DrupipeContainer(container)
                container.controller = controller
                container.pod = this
                container.execute()
            }
        }

    }

}
