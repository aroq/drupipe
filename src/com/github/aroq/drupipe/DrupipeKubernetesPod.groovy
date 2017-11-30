package com.github.aroq.drupipe

class DrupipeKubernetesPod implements Serializable {

    String name

    ArrayList unstash = []

    ArrayList stash = []

    ArrayList<DrupipeContainer> containers = []

    DrupipeController controller

    boolean containerized = true

    boolean unipipe_retrieve_config = false

    def execute(body = null) {
        def script = controller.script
        script.echo "DrupipePod execute - ${name}"
        script.drupipeExecuteKubernetesContainers(containers, controller, unstash, stash, unipipe_retrieve_config)
    }

}
