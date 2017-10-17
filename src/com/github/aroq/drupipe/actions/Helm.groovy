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
        // Prepare params.
        def params = this.action.params
        params.working_dir = this.script.pwd()

        // Prepare flags.
//        params.flags = prepareFlags(params.flags)

        // Execute helm command.
//        String command = [params.executable, params.command, params.flags, params.release_name, params.chart_dir].join(' ')
//        String command = [params.executable, params.command, params.flags, params.release_name, params.chart_dir].join(' ')

        this.script.withEnv(["KUBECONFIG=${params.working_dir}/${params.kubectl_config_file}"]) {
            this.script.drupipeShell("""
                ${params.full_command.join(' ')} 
            """, this.context << [shellCommandWithBashLogin: false])
        }
    }

    def status() {
        executeHelmCommand()
    }

    def delete() {
        executeHelmCommand()
    }

    @NonCPS
    def prepareFlags(flags) {
        flags.collect { k, v ->
            v.collect { subItem ->
                "${k} ${subItem}".trim()
            }.join(' ').trim()
        }.join(' ')
    }

}

