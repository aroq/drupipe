package com.github.aroq.drupipe

class DrupipeKubernetesPod implements Serializable {

    String name

    ArrayList<DrupipeContainer> containers = []

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipePod execute - ${name}"

        script.drupipeExecuteKubernetesContainers(containers, controller)

//        for (container in containers) {
//            controller.utils.debugLog(controller.context, container, 'CONTAINER', [debugMode: 'json'], [], true)
//            container = new DrupipeContainer(container)
//            container.controller = controller
//            container.execute()
//        }
    }

}
