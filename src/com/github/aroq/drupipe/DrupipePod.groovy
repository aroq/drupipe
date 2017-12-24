package com.github.aroq.drupipe

class DrupipePod extends DrupipeBase {

    String name

    ArrayList unstash = []

    ArrayList stash = []

    ArrayList<DrupipeContainer> pre_containers = []
    ArrayList<DrupipeContainer> containers = []
    ArrayList<DrupipeContainer> post_containers = []
    ArrayList<DrupipeContainer> final_containers = []

    ArrayList phases = ['pre_containers', 'containers', 'post_containers', 'final_containers']

    boolean containerized = true

    boolean unipipe_retrieve_config = false

    ArrayList<DrupipeContainer> prepareContainers() {
        ArrayList<DrupipeContainer> result = []
        for (phase in phases) {
            if (this."${phase}") {
                controller.drupipeLogger.trace "Execute POD phase: ${phase}"
                for (container in this."${phase}") {
                    if (container.execute) {
                        container.remove('execute')
                        controller.drupipeLogger.debugLog(controller.context, container, 'CONTAINER', [debugMode: 'json'], [], 'DEBUG')
                        container = new DrupipeContainer(container)
                        container.controller = controller
                        container.pod = this
                        result.add(container)
                    }
                    else {
                        controller.drupipeLogger.debug "Container ${name} 'execute' property is set to false"
                    }
                }
            }
        }
        result
    }

    def executeContainers() {
        for (container in containers) {
            container.execute()
        }
    }

    def execute(body = null) {
        def script = controller.script
        controller.drupipeLogger.trace "DrupipePod execute - ${name}"

        script.node(name) {
            if (unipipe_retrieve_config) {
                controller.utils.getUnipipeConfig(controller)
            }
            else {
                controller.drupipeLogger.warning "Retrieve config disabled in config."
            }
            controller.utils.unstashList(controller, unstash)

            executeContainers()

            controller.utils.stashList(controller, stash)
        }
    }

}
