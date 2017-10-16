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

    def apply() {
        // Prepare params
        String valueFileSuffix = 'values.yaml'
        String helmChartsDir = 'charts'

        String helmChartName = this.context.jenkinsParams.chartName
        String helmChartDir = helmChartsDir + '/' + helmChartName
        String helmEnv = this.context.jenkinsParams.helmEnv
        String helmReleaseName = this.context.jenkinsParams.helmReleaseName ? this.context.jenkinsParams.helmReleaseName : "${helmChartName}-${helmEnv}"
        String k8sNamespace = this.context.jenkinsParams.k8sNamespace ? this.context.jenkinsParams.k8sNamespace : "${helmReleaseName}-${helmEnv}"
        String valuesFile = "${helmReleaseName}.${valueFileSuffix}"
        String envValuesFile = "${helmEnv}.${helmReleaseName}.${valueFileSuffix}"
        String workingDir = this.script.pwd()
        String helmExecutable = this.action.params.helmExecutable
        String helmCommand = this.action.params.helmCommand

        this.action.params.helmFlags << [namespace: k8sNamespace]
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

