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
        def creds = [script.file(credentialsId: params.secret_values_file_id, variable: params.secret_values_file_id)]
        script.withCredentials(creds) {
            this.script.withEnv(["KUBECONFIG=${params.working_dir}/${params.kubectl_config_file}"]) {
                this.script.drupipeShell("""
                    ${command} 
                """, this.context << [shellCommandWithBashLogin: false])
            }
        }

    }

    def status() {
        executeHelmCommand()
//        String helmChartName   = utils.getActionParam('helmChartName',   this.action.params, this.context.jenkinsParams)
//        String helmEnv         = utils.getActionParam('helmEnv',         this.action.params, this.context.jenkinsParams)
//        String helmReleaseName = utils.getActionParam('helmReleaseName', this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
//        String helmExecutable  = utils.getActionParam('helmExecutable',  this.action.params, this.context.jenkinsParams)
//        String helmCommand     = utils.getActionParam('helmCommand',     this.action.params, this.context.jenkinsParams)
//        String kubeConfigFile  = utils.getActionParam('kubeConfigFile',  this.action.params, this.context.jenkinsParams)
//
//        String workingDir      = this.script.pwd()
//
//        // Prepare flags.
//        def helmFlags = prepareFlags(this.action.params.helmFlags)
//
//        this.script.withEnv(["KUBECONFIG=${workingDir}/${kubeConfigFile}"]) {
//            this.script.drupipeShell("""
//            ${helmExecutable} ${helmCommand} ${helmFlags} ${helmReleaseName}
//            """, this.context << [shellCommandWithBashLogin: false])
//        }
    }

    def delete() {
        String helmChartName   = utils.getActionParam('helmChartName',   this.action.params, this.context.jenkinsParams)
        String helmEnv         = utils.getActionParam('helmEnv',         this.action.params, this.context.jenkinsParams)
        String helmReleaseName = utils.getActionParam('helmReleaseName', this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
        String helmExecutable  = utils.getActionParam('helmExecutable',  this.action.params, this.context.jenkinsParams)
        String helmCommand     = utils.getActionParam('helmCommand',     this.action.params, this.context.jenkinsParams)
        String kubeConfigFile  = utils.getActionParam('kubeConfigFile',  this.action.params, this.context.jenkinsParams)

        String workingDir      = this.script.pwd()

        // Prepare flags.
        def helmFlags= prepareFlags(this.action.params.helmFlags)

        this.script.withEnv(["KUBECONFIG=${workingDir}/${kubeConfigFile}"]) {
            this.script.drupipeShell("""
            ${helmExecutable} ${helmCommand} ${helmFlags} ${helmReleaseName}
            """, this.context << [shellCommandWithBashLogin: false])
        }
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

