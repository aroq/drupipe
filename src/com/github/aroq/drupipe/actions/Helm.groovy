package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Helm extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def config() {
        this.script.echo "Helm.config"
    }

    def status() {
        this.script.withEnv(["KUBECONFIG=${this.action.params.workingDir}/.kubeconfig"]) {
            kubectl get pods
            this.script.drupipeShell("""
            """, this.context << [shellCommandWithBashLogin: false])
        }
    }

    def install() {
        this.script.echo "Helm.install"
    }

    def delete() {
        this.script.echo "Helm.delete"
    }

}

