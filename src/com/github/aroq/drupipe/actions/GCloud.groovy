package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class GCloud extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def auth() {
         this.script.drupipeShell("""
              gcloud auth activate-service-account --key-file \${${action.params.access_key_file_id}}
              gcloud config set compute/zone europe-west1-b
              gcloud config set project zebra-aroq
              gcloud config set container/use_client_certificate True
              gcloud container clusters get-credentials main
        """, this.context << [shellCommandWithBashLogin: false])
    }

}

