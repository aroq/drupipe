package com.github.aroq.drupipe

class DrupipeKubernetesPod extends DrupipeBase {

    String name

    ArrayList unstash = []

    ArrayList stash = []

    ArrayList<DrupipeContainer> containers = []

    boolean containerized = true

    boolean unipipe_retrieve_config = false

    def execute(body = null) {
        executeWithCollapsed("POD: ${name}") {
            controller.script.drupipeExecuteKubernetesContainers(containers, controller, unstash, stash, unipipe_retrieve_config)
        }
    }

}
