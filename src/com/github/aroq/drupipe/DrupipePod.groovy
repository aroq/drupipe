package com.github.aroq.drupipe

class DrupipePod extends DrupipeBase {

    String name

    ArrayList unstash = []

    ArrayList stash = []

    ArrayList<DrupipeContainer> containers = []

    DrupipeController controller

    boolean containerized = true

    boolean unipipe_retrieve_config = false

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
            for (container in containers) {
                controller.drupipeLogger.debugLog(controller.context, container, 'CONTAINER', [debugMode: 'json'])
                container = new DrupipeContainer(container)
                container.controller = controller
                container.pod = this
                container.execute()
            }
            controller.utils.stashList(controller, stash)
        }

    }

}
