package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Jenkins extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def cli() {
        def inventory = script.readJSON file: 'terraform.inventory.json'
        def creds = [this.script.file(credentialsId: 'id_rsa', variable: 'ZEBRA_ID_RSA')]
        this.script.withCredentials(creds) {
            this.script.withEnv(["JENKINS_URL=http://${inventory['zebra-jenkins-master'][0]}:${this.action.params.port}", "PRIVATE_KEY=${ZEBRA_ID_RSA}"]) {
                this.script.drupipeShell("""
                chmod 400 ${ZEBRA_ID_RSA}
                /jenkins-cli/jenkins-cli-wrapper.sh help
                """, this.context)
            }
        }
    }

}
