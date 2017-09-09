package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Jenkins extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def cli() {
        def inventory = script.readJSON file: 'terraform.inventory.json'
        def creds = [this.script.file(credentialsId: 'id_rsa', variable: 'PRIVATE_KEY')]
        this.script.withCredentials(creds) {
            def envvars = ["JENKINS_URL=http://${inventory['zebra-jenkins-master'][0]}:${this.action.params.port}"]
            this.script.withEnv(envvars) {
                this.script.drupipeShell('chmod 400 ${PRIVATE_KEY}', this.context)
                this.script.drupipeShell('''
                /jenkins-cli/jenkins-cli-wrapper.sh -i ${PRIVATE_KEY} help
                ''', this.context)
            }
        }
    }

}
