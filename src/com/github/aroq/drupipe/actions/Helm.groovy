package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Helm extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def init() {
        this.script.echo "Helm.init"
    }

    // Apply Helm chart idempotently.
    def apply() {
        executeHelmCommand()
    }

    def executeHelmCommand() {
//        this.script.withEnv(["KUBECONFIG=${this.context.drupipe_working_dir}/${this.action.params.kubectl_config_file}"]) {
            this.script.drupipeShell("""
                ${this.action.params.full_command.join(' ')} 
            """, this.context << [shellCommandWithBashLogin: false])
//        }
    }

    def status() {
        executeHelmCommand()
    }

    def delete() {
        executeHelmCommand()
    }

}

