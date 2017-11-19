package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    String name

    String from

    ArrayList<DrupipePod> pre_pods = []

    ArrayList<DrupipePod> pods = []

    ArrayList<DrupipePod> post_pods = []

    ArrayList<DrupipePod> final_pods = []

    ArrayList phases = ['pre_pods', 'pods', 'post_pods', 'final_pods']

    DrupipeController controller

    def execute(body = null) {
        def script = controller.script
        controller.script.echo "DrupipePipeline execute - ${name}"
        // TODO: add try/finally for 'final_pods' execution.
        for (phase in phases) {
            if (this."${phase}") {
                script.echo "Execute PIPELINE phase: ${phase}"
                for (pod in this."${phase}") {
                    executePod(pod)
                }
            }
        }
    }

    def executePod(pod) {
        if (pod) {
            controller.utils.debugLog(controller.context, pod, 'POD', [debugMode: 'json'], [], true)

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
