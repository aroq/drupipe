package com.github.aroq.drupipe

class DrupipeKubernetesPod extends DrupipePod {

    def execute(body = null) {
        executeWithCollapsed("POD: ${name}") {
            controller.script.drupipeExecuteKubernetesPod(this, unstash, stash, unipipe_retrieve_config)
        }
    }

}
