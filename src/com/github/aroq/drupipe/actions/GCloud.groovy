package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class GCloud extends BaseAction {

    def auth() {
        if (action.pipeline.context.containerMode != 'kubernetes') {

            // TODO: Make it centralized.
            String access_key_file
            if (action.pipeline.context.containerMode == 'kubernetes') {

                this.script.drupipeShell(
"""
echo "\${${action.params.access_key_file_id}}" > .google_access_key_file
""", this.action.params)

                access_key_file = action.params.workingDir != '.' ? '.google_access_key_file' : '.google_access_key_file'
            }
            else {
                access_key_file = "\${${action.params.access_key_file_id}}"
            }

            this.script.drupipeShell(
"""
${action.params.executable} auth activate-service-account --key-file ${access_key_file}
${action.params.executable} config set compute/zone ${action.params.compute_zone}
${action.params.executable} config set project ${action.params.project_name} 
${action.params.executable} config set container/use_client_certificate True
${action.params.executable} container clusters get-credentials ${action.params.cluster_name} 
""", action.params)

        }

    }

}

