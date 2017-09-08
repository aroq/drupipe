package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Terraform extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    String terraformExecutable = 'terraform'

    def init() {
        def sourceDir = utils.sourceDir(context, action.params.infraSourceName)
        script.drupipeShell("""
            cd ${sourceDir}
            ${terraformExecutable} init -input=false
            """, context)
    }

    def plan() {
        def sourceDir = utils.sourceDir(context, action.params.infraSourceName)
        def creds = script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')
        script.withCredentials([creds]) {
            drupipeShell("""
            cd ${sourceDir}
            ${terraformExecutable} plan -auto-approve=true -input=false -state=terraform/dev/terraform.tfstate -var-file=terraform/dev/terraform.tfvars -var-file=terraform/dev/secrets.tfvars
            """, context)
        }
    }

    def apply() {

    }

    def destroy() {

    }
}
