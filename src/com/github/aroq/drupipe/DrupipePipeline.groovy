package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    String name

    String from

    ArrayList<DrupipePod> pods = []

    DrupipeController controller

    def execute(body = null) {
        controller.script.echo "DrupipePipeline execute - ${name}"
        for (pod in pods) {
            controller.utils.debugLog(controller.context, pod, 'POD', [debugMode: 'json'], [], true)
            pod = new DrupipePod(pod)
            pod.controller = controller
            pod.execute()
        }
    }

}
