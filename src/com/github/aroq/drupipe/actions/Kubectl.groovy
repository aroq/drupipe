package com.github.aroq.drupipe.actions

class Kubectl extends BaseShellAction {

    def scale_replicaset() {
        executeShellCommand()
    }

    def scale_down_up() {
        def name = action.pipeline.executeAction(action: 'Kubectl.get_replicaset_name').stdout
        script.echo "Replicaset name: ${name}"
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

    def get_pod_name() {
        executeShellCommand()
    }

    def get_pod_logs() {
        executeShellCommand()
    }

    def get_loadbalancer_address() {
        [
            url: executeShellCommand().stdout,
        ]
    }

    def get_replicaset_name() {
        executeShellCommand()
    }

    def get_pods() {
        executeShellCommand()
    }

    def copy_from_pod() {
        executeShellCommand()
    }

}

