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
        params.flags = prepareFlags(params.flags)

        // Execute helm command.
        // TODO: Make sure only allowed credentials could be used. Control it with projects.yaml in mothership config.
        String command = [params.executable, params.command, params.flags, params.release_name, params.chart_dir].join(' ')
//        def creds = [script.file(credentialsId: params.secret_values_file_id, variable: params.secret_values_file_id)]

        // Process credentials.
        ArrayList credentials = []
        if (params.credentials) {
            params.credentials.each { k, v ->
                if (v.type == 'file') {
                    v.variable_name = v.variable_name ? v.variable_name : v.id
                    credentials << this.script.file(credentialsId: v.id, variable: v.variable_name)
                }
            }
        }

        script.withCredentials(credentials) {
            this.script.withEnv(["KUBECONFIG=${params.working_dir}/${params.kubectl_config_file}"]) {
                this.script.drupipeShell("""
                    ${command} 
                """, this.context << [shellCommandWithBashLogin: false])
            }
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

