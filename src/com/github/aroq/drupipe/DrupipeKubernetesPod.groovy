package com.github.aroq.drupipe

class DrupipeKubernetesPod extends DrupipeBase {

    String name

    ArrayList unstash = []

    ArrayList stash = []

    ArrayList<DrupipeContainer> containers = []

    DrupipeController controller

    boolean containerized = true

    boolean unipipe_retrieve_config = false

    def execute(body = null) {
        controller.drupipeLogger.collapsedStart("POD EXECUTION START - ${name}")
        controller.script.drupipeExecuteKubernetesContainers(containers, controller, unstash, stash, unipipe_retrieve_config)
        controller.drupipeLogger.collapsedStart("POD EXECUTION END - ${name}")
    }

}
