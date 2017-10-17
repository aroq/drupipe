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
        // Prepare params.
//        String valueFileSuffix = utils.getActionParam('helmValueFileSuffix', this.action.params, this.context.jenkinsParams)
//        String helmChartsDir   = utils.getActionParam('helmChartsDir',       this.action.params, this.context.jenkinsParams)
//        String helmChartName   = utils.getActionParam('helmChartName',       this.action.params, this.context.jenkinsParams)
//        String helmEnv         = utils.getActionParam('helmEnv',             this.action.params, this.context.jenkinsParams)
//        String helmReleaseName = utils.getActionParam('helmReleaseName',     this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
//        String helmNamespace   = utils.getActionParam('helmNamespace',       this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
//        String helmExecutable  = utils.getActionParam('helmExecutable',      this.action.params, this.context.jenkinsParams)
//        String helmCommand     = utils.getActionParam('helmCommand',         this.action.params, this.context.jenkinsParams)
//        String kubeConfigFile  = utils.getActionParam('kubeConfigFile',      this.action.params, this.context.jenkinsParams)

//        String valuesFile         = [helmChartName, valueFileSuffix].join('.')
//        String envValuesFile      = [helmEnv, helmChartName, valueFileSuffix].join('.')
//        String secretsValuesFile  = "\${HELM_ZEBRA_SECRETS_FILE}"
//        String helmChartDir       = [helmChartsDir, helmChartName].join('/')

        String workingDir = this.script.pwd()

        def params = this.action.params

        // Prepare flags.
        params.flags << [
            '--namespace': [params.namespace],
            '-f': [params.values_file, params.env_values_file, "\${${params.secret_values_file_id}}"]
        ]
        params.flags = prepareFlags(params.flags)

        // Execute helm command.
        def creds = [script.file(credentialsId: params.secret_values_file, variable: params.secret_values_file)]
        script.withCredentials(creds) {
            this.script.withEnv(["KUBECONFIG=${workingDir}/${params.kubeconfig_file}"]) {
                this.script.drupipeShell("""
                     ${params.executable} ${params.command} ${params.flags} ${params.release_name} ${params.chart_dir}
                """, this.context << [shellCommandWithBashLogin: false])
            }
        }
    }

    def status() {
        String helmChartName   = utils.getActionParam('helmChartName',   this.action.params, this.context.jenkinsParams)
        String helmEnv         = utils.getActionParam('helmEnv',         this.action.params, this.context.jenkinsParams)
        String helmReleaseName = utils.getActionParam('helmReleaseName', this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
        String helmExecutable  = utils.getActionParam('helmExecutable',  this.action.params, this.context.jenkinsParams)
        String helmCommand     = utils.getActionParam('helmCommand',     this.action.params, this.context.jenkinsParams)
        String kubeConfigFile  = utils.getActionParam('kubeConfigFile',  this.action.params, this.context.jenkinsParams)

        String workingDir      = this.script.pwd()

        // Prepare flags.
        def helmFlags = prepareFlags(this.action.params.helmFlags)

        this.script.withEnv(["KUBECONFIG=${workingDir}/${kubeConfigFile}"]) {
            this.script.drupipeShell("""
            ${helmExecutable} ${helmCommand} ${helmFlags} ${helmReleaseName}
            """, this.context << [shellCommandWithBashLogin: false])
        }
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

