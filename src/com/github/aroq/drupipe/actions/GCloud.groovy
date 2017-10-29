package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class GCloud extends BaseAction {

    def context

    def script

    def utils

    DrupipeActionConroller action

    def auth() {
         this.script.drupipeShell("""
              ${action.params.executable} auth activate-service-account --key-file \${${action.params.access_key_file_id}}

              ${action.params.executable} config set compute/zone ${action.params.compute_zone}
              ${action.params.executable} config set project ${action.params.project_name} 
              ${action.params.executable} config set container/use_client_certificate True

              ${action.params.executable} container clusters get-credentials ${action.params.cluster_name} 
        """, action.params)
    }

}

