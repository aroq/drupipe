package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Terraform extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    String terraformExecutable = 'terraform'

    def initializeAction() {
        if (context.jenkinsParams.containsKey('workingDir')) {
            action.params.workingDir = context.jenkinsParams.workingDir
        }
        else {
            action.params.workingDir = '.'
        }
    }

    def init() {
        initializeAction()
        def creds = script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN')
        this.script.withCredentials([creds]) {
            this.script.drupipeShell("""
            cd ${this.action.params.workingDir}
            ${terraformExecutable} init -input=false -backend-config="address=${this.context.env.TF_VAR_consul_address}" -backend-config="access_token=\${CONSUL_ACCESS_TOKEN}"

            """, this.context)
        }
    }

    def state() {
        initializeAction()
        script.drupipeShell("""
            cd ${this.action.params.workingDir}
            /usr/bin/terraform-inventory --list > ${action.params.stateFile}
            """, context)
        this.script.stash name: 'terraform-state}', includes: "${action.params.stateFile}"
    }

    def executeTerraformCommand(String terraformCommand) {
        String terraformEnv = this.context.jenkinsParams.terraformEnv
        String terraformWorkspace = this.context.jenkinsParams.terraformEnv ? this.context.jenkinsParams.terraformEnv : 'default'

        initializeAction()

        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN'), script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
        script.withCredentials(creds) {
            this.script.drupipeShell("""
            cd ${this.action.params.workingDir}
            TF_WORKSPACE=${terraformWorkspace} TF_VAR_consul_access_token=\$CONSUL_ACCESS_TOKEN ${this.terraformExecutable} ${terraformCommand} -var-file=terraform/${terraformEnv}/terraform.tfvars -var-file=terraform/${terraformEnv}/secrets.tfvars
            """, this.context)
        }
    }

    def plan() {
        executeTerraformCommand('plan')
    }

    def apply() {
        executeTerraformCommand('apply')
    }

    // TODO: refactor it to use executeTerraformCommand().
    def destroy() {
        String terraformEnv = this.context.jenkinsParams.terraformEnv
        String terraformWorkspace = this.context.jenkinsParams.terraformEnv ? this.context.jenkinsParams.terraformEnv : 'default'

        initializeAction()

        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN'), script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
        script.withCredentials(creds) {
            this.script.drupipeShell("""
            cd ${this.action.params.workingDir}
            TF_WORKSPACE=${terraformWorkspace} TF_VAR_consul_access_token=\$CONSUL_ACCESS_TOKEN ${this.terraformExecutable} destroy -force=true -input=false -var-file=terraform/${terraformEnv}/terraform.tfvars -var-file=terraform/${terraformEnv}/secrets.tfvars
            """, this.context)
        }
    }

}
