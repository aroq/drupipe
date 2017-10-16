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
        String valueFileSuffix = utils.getActionParam('helmValueFileSuffix', this.action.params, this.context.jenkinsParams)
        String helmChartsDir   = utils.getActionParam('helmChartsDir',       this.action.params, this.context.jenkinsParams)
        String helmChartName   = utils.getActionParam('helmChartName',       this.action.params, this.context.jenkinsParams)
        String helmEnv         = utils.getActionParam('helmEnv',             this.action.params, this.context.jenkinsParams)
        String helmReleaseName = utils.getActionParam('helmReleaseName',     this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
        String helmNamespace   = utils.getActionParam('helmNamespace',       this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
        String helmExecutable  = utils.getActionParam('helmExecutable',      this.action.params, this.context.jenkinsParams)
        String helmCommand     = utils.getActionParam('helmCommand',         this.action.params, this.context.jenkinsParams)
        String kubeConfigFile  = utils.getActionParam('kubeConfigFile',      this.action.params, this.context.jenkinsParams)

        String valuesFile      = [helmChartName, valueFileSuffix].join('.')
        String envValuesFile   = [helmEnv, helmChartName, valueFileSuffix].join('.')
        String helmChartDir    = [helmChartsDir, helmChartName].join('/')

        String workingDir      = this.script.pwd()

        // Prepare flags.
        this.action.params.helmFlags << [
            '--namespace': [helmNamespace],
            '-f': [valuesFile, envValuesFile, "\${HELM_ZEBRA_SECRETS_FILE}"]
        ]
        def helmFlags= prepareFlags(this.action.params.helmFlags)

        def creds = [script.file(credentialsId: 'HELM_ZEBRA_SECRETS_FILE', variable: 'HELM_ZEBRA_SECRETS_FILE')]
        script.withCredentials(creds) {
            this.script.withEnv(["KUBECONFIG=${workingDir}/${kubeConfigFile}"]) {
                this.script.drupipeShell("""
                     ${helmExecutable} ${helmCommand} ${helmFlags} ${helmReleaseName} ${helmChartDir}
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

