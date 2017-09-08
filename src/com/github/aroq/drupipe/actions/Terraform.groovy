package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Terraform extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    String terraformExecutable = 'terraform'

    def init() {
        script.drupipeShell("""
            ${terraformExecutable} init -input=false
            """, context)
    }

    def plan() {
        def sourceDir = utils.sourceDir(context, action.params.infraSourceName)
        def creds = script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')
        script.withCredentials([creds]) {
            this.script.drupipeShell("""
            cd ${sourceDir}
            ${this.terraformExecutable} plan -state=terraform/dev/terraform.tfstate -var-file=terraform/dev/terraform.tfvars -var-file=terraform/dev/secrets.tfvars
            """, this.context)
        }
    }

    def apply() {
        def sourceDir = utils.sourceDir(context, action.params.infraSourceName)
        def creds = script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')
        script.withCredentials([creds]) {
            this.script.drupipeShell("""
            cd ${sourceDir}
            ${this.terraformExecutable} apply -auto-approve=true -input=false -state=terraform/dev/terraform.tfstate -var-file=terraform/dev/terraform.tfvars -var-file=terraform/dev/secrets.tfvars
            """, this.context)
        }

    }

    def destroy() {

    }
}
