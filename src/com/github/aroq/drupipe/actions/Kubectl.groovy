package com.github.aroq.drupipe.actions

class Kubectl extends BaseShellAction {

    def scale_down_up() {
        def name = action.pipeline.executeAction(action: 'Kubectl.get_replicaset_name').stdout
        action.pipeline.drupipeLogger.warning "Replicaset name: ${name}"
        action.pipeline.drupipeLogger.warning "Replicaset replicas_down: ${action.params.replicas_down}"
        action.pipeline.drupipeLogger.warning "Replicaset replicas_up: ${replicas: action.params.replicas_up}"
        action.pipeline.executeAction(
            action: 'Kubectl.scale_replicaset',
            params: [replicaset_name: name, replicas: action.params.replicas_down]
        )
        action.pipeline.executeAction(
            action: 'Kubectl.scale_replicaset',
            params: [replicaset_name: name, replicas: action.params.replicas_up]
        )
        // TODO: remove it when get pod command will take 'status' field into account (e.g. Running, etc).
        script.drupipeShell("sleep 30", action.params)
    }

    def get_loadbalancer_address() {
        [
            url: execute().stdout,
        ]
    }

}

