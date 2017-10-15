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

    def install() {
        action.params.workingDir = this.script.pwd()
        String helmEnv = this.context.jenkinsParams.helmEnv
        action.params.valuesFile = 'zebra.values.yaml'

        def creds = [script.file(credentialsId: 'HELM_ZEBRA_SECRETS_FILE', variable: 'HELM_ZEBRA_SECRETS_FILE')]
        script.withCredentials(creds) {
            this.script.withEnv(["KUBECONFIG=${this.action.params.workingDir}/.kubeconfig"]) {
                this.script.drupipeShell("""
                helm install --wait --timeout 120 --name zebra-cd-${helmEnv} . -f ${this.action.params.valuesFile} -f \${HELM_ZEBRA_SECRETS_FILE}
                """, this.context << [shellCommandWithBashLogin: false])
            }
        }
    }

    def delete() {
        this.script.echo "Helm.delete"
    }

}

