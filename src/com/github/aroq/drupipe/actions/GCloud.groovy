package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class GCloud extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def auth() {
        this.script.echo "gcloud.auth"

        action.params.workingDir = this.script.pwd()

        def creds = [script.file(credentialsId: 'GCLOUD_ACCESS_KEY', variable: 'GCLOUD_ACCESS_KEY')]
        script.withCredentials(creds) {
            this.script.withEnv("KUBECONFIG=${this.action.params.workingDir}") {
                this.script.drupipeShell("""
                      gcloud auth activate-service-account --key-file ${GCLOUD_ACCESS_KEY}
                      gcloud config set compute/zone europe-west1-b
                      gcloud config set project zebra-aroq
                      gcloud config set container/use_client_certificate True
                      gcloud container clusters get-credentials main
                      ls -al ${action.params.workingDir}
            """, this.context << [shellCommandWithBashLogin: false])
            }
        }
    }

}

