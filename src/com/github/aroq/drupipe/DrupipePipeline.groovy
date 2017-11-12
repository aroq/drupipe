package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    String name

    String from

    ArrayList<DrupipePod> pods = []

    DrupipeController controller

    def execute(body = null) {
        controller.script.echo "DrupipePipeline execute - ${name}"
        for (pod in pods) {
//            controller.utils.debugLog(controller.context, pod, 'POD', [debugMode: 'json'], [], true)

            if (pod.containerized && controller.context.containerMode == 'kubernetes') {
                pod = new DrupipeKubernetesPod(pod)
            }
            else {
                pod = new DrupipePod(pod)
            }
            pod.controller = controller
            pod.execute()
        }
    }

}
