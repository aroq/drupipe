package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper
import com.github.aroq.drupipe.DrupipePipeline

class Kubectl extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def scale_replicaset() {
        executeKubectlCommand()
    }

    def scale_down_up() {
        def name = script.drupipeAction([action: "Kubectl.get_replicaset_name"], action.pipeline).stdout
        script.echo "Replicaset name: ${name}"
        script.drupipeAction([action: "Kubectl.scale_replicaset", params: [name: name, replicas: action.pipeline.params.replicas_down]], action.pipeline)
        script.drupipeAction([action: "Kubectl.scale_replicaset", params: [name: name, replicas: action.pipeline.params.replicas_up]], action.pipeline)
    }

    def get_pod_name() {
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
    }

    def executeKubectlCommand() {
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

