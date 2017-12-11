package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Kubectl extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def scale_replicaset() {
        executeKubectlCommand()
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
        executeKubectlCommand()
    }

    def get_pod_logs() {
        executeKubectlCommand()
    }

    def get_loadbalancer_address() {
        [
            url: executeKubectlCommand().stdout,
        ]
    }

    def get_replicaset_name() {
        executeKubectlCommand()
    }

    def get_pods() {
        executeKubectlCommand()
    }

    def copy_from_pod() {
        executeKubectlCommand()
        script.drupipeShell("ls -al", action.params)
    }

    def executeKubectlCommand() {
        if (!action.params.full_command) {
            controller.drupipeLogger.debugLog(action.pipeline.context, action.params, "ACTION PARAMS (full_command is absent)", [debugMode: 'json'])
        }
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

