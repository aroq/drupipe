package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Terraform extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    String terraformExecutable = 'terraform'

    def init() {
        def creds = script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN')
        this.script.withCredentials([creds]) {
            this.script.drupipeShell("""
            ${terraformExecutable} init -input=false -backend-config="address=${this.context.env.TF_VAR_consul_address}" -backend-config="access_token=\${CONSUL_ACCESS_TOKEN}"

            """, this.context)
        }
    }

    def state() {
        script.drupipeShell("""
            /usr/bin/terraform-inventory --list > ${action.params.stateFile}
            """, context)
        this.script.stash name: 'terraform-state}', includes: "${action.params.stateFile}"
    }

    def executeTerraformCommand(String terraformCommand) {
        String terraformEnv = this.context.jenkinsParams.terraformEnv
        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN'), script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
        script.withCredentials(creds) {
            this.script.drupipeShell("""
            TF_VAR_consul_access_token=\$CONSUL_ACCESS_TOKEN ${this.terraformExecutable} ${terraformCommand} -var-file=terraform/${terraformEnv}/terraform.tfvars -var-file=terraform/${terraformEnv}/secrets.tfvars
            """, this.context)
        }
    }

    def plan() {
        executeTerraformCommand('plan')
    }

    def apply() {
        executeTerraformCommand('apply')
    }

    def destroy() {
        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN'), script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
        script.withCredentials(creds) {
            this.script.drupipeShell("""
            TF_VAR_consul_access_token=\$CONSUL_ACCESS_TOKEN ${this.terraformExecutable} destroy -force=true -input=false -var-file=terraform/${terraformEnv}/terraform.tfvars -var-file=terraform/${terraformEnv}/secrets.tfvars
            """, this.context)
        }
    }

}
