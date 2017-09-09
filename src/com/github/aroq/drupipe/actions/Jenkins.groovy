package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Jenkins extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def cli() {

        def inventory = script.readJSON file: 'terraform.inventory.json'

        def sourceDir = utils.sourceDir(context, action.params.infraSourceName)
        this.script.withEnv(["JENKINS_URL=${inventory['zebra-jenkins-master'][0]}"]) {
            this.script.drupipeShell("""
            cd ${sourceDir}
            jenkins-cli-wrapper.sh help
            """, this.context)

        }
    }

}
