package com.github.aroq.drupipe

class DrupipePod implements Serializable {

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

        script.node(name) {
            if (unipipe_retrieve_config) {
                controller.utils.getUnipipeConfig(controller)
            }
            controller.utils.unstashList(unstash)
            for (container in containers) {
//                controller.utils.debugLog(controller.context, container, 'CONTAINER', [debugMode: 'json'], [], true)
                container = new DrupipeContainer(container)
                container.controller = controller
                container.pod = this
                container.execute()
            }
            controller.utils.stashList(stash)
        }

    }

}
