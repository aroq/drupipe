package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class GCloud extends BaseAction {

    def context

    def script

    def utils

    DrupipeAction action

    def auth() {
         this.script.drupipeShell("""
              gcloud auth activate-service-account --key-file \${${action.params.access_key_file_id}}
              gcloud config set compute/zone ${action.params.compute_zone}
              gcloud config set project ${action.params.project_name} 
              gcloud config set container/use_client_certificate True
              gcloud container clusters get-credentials ${action.params.cluster_name} 
        """, this.context << [shellCommandWithBashLogin: false])
    }

}

