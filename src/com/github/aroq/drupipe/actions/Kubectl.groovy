package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Kubectl extends BaseAction {

    def context

    def script

    def utils

    DrupipeAction action

    def scale_replicaset() {
        executeKubectlCommand()
    }

    def scale_down_up() {
        def name = script.drupipeAction([action: "Kubectl.get_replicaset_name"], context).drupipeShellResult
        script.echo "Replicaset name: ${name}"
        script.drupipeAction([action: "Kubectl.scale_replicaset", params: [name: name, replicas: action.params.replicas_down]], context)
        script.drupipeAction([action: "Kubectl.scale_replicaset", params: [name: name, replicas: action.params.replicas_up]], context)
    }

    def get_pod_name() {
        executeKubectlCommand()
    }

    def get_loadbalancer_address() {
        [
            url: executeKubectlCommand().drupipeShellResult,
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
    }

    def executeKubectlCommand() {
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

