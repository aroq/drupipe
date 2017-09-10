package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Jenkins extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def cli() {
        def inventory = script.readJSON file: 'terraform.inventory.json'
        def creds = [this.script.string(credentialsId: 'JENKINS_API_TOKEN', variable: 'JENKINS_API_TOKEN')]
        this.script.withCredentials(creds) {
            def envvars = ["JENKINS_URL=http://${inventory['zebra-jenkins-master'][0]}:${this.action.params.port}"]
            this.script.withEnv(envvars) {
                this.script.drupipeShell("""
                /jenkins-cli/jenkins-cli-wrapper.sh -auth ${this.action.params.user}:\${JENKINS_API_TOKEN} ${this.action.params.command}
                """, this.context)
            }
        }
    }

    def build() {
        this.action.params.command = "build -s -v ${this.action.params.jobName}"
        cli()
    }

    def seedTest() {
        def sourceDir = utils.sourceDir(context, 'mothership')
        def json = this.script.readFile("${sourceDir}/projects.json")
        def projects = json.find {k, v -> v.containsKey('tests') && v['tests'].contains('seed') }
        projects.each {k, v ->
            this.action.params.jobName = "${k}/seed"
            build()
        }
    }

}
