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
        action.params.workingDir = this.script.pwd()

        this.script.withEnv(["KUBECONFIG=${this.action.params.workingDir}/.kubeconfig"]) {
            this.script.drupipeShell("""
            kubectl get pods
            """, this.context << [shellCommandWithBashLogin: false])
        }
    }

    // Idempotent command to apply Helm chart.
    def apply() {
        // Prepare params.
        String valueFileSuffix = 'values.yaml'
        String helmChartsDir   = 'charts'

        String helmChartName   = utils.getActionParam('helmChartName',   this.action.params, this.context.jenkinsParams)
        String helmEnv         = utils.getActionParam('helmEnv',         this.action.params, this.context.jenkinsParams)
        String helmReleaseName = utils.getActionParam('helmReleaseName', this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
        String helmNamespace   = utils.getActionParam('helmNamespace',   this.action.params, this.context.jenkinsParams, [helmChartName, helmEnv].join('-'))
        String helmExecutable  = utils.getActionParam('helmExecutable',  this.action.params, this.context.jenkinsParams)
        String helmCommand     = utils.getActionParam('helmCommand',     this.action.params, this.context.jenkinsParams)

        String valuesFile      = [helmChartName, valueFileSuffix].join('.')
        String envValuesFile   = [helmEnv, helmChartName,valueFileSuffix].join('.')
        String helmChartDir    = [helmChartsDir, helmChartName].join('/')

        String workingDir      = this.script.pwd()

        // Prepare flags.
        this.action.params.helmFlags << [namespace: helmNamespace]
        def helmFlags= prepareFlags(this.action.params.helmFlags)

        def creds = [script.file(credentialsId: 'HELM_ZEBRA_SECRETS_FILE', variable: 'HELM_ZEBRA_SECRETS_FILE')]
        script.withCredentials(creds) {
            this.script.withEnv(["KUBECONFIG=${workingDir}/.kubeconfig"]) {
                this.script.drupipeShell("""
                ${helmExecutable} ${helmCommand} ${helmFlags} ${helmReleaseName} ${helmChartDir} \
                    -f ${valuesFile} \
                    -f ${envValuesFile} \
                    -f \${HELM_ZEBRA_SECRETS_FILE}
                """, this.context << [shellCommandWithBashLogin: false])
            }
        }
    }

    def delete() {
        this.script.echo "Helm.delete"
    }

    @NonCPS
    prepareFlags(flags) {
        def result = []
        flags.each {k, v ->
            result << "--${k} ${v}".trim()
        }

        result.join(' ')
    }

}

