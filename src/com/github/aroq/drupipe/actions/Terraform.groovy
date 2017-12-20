package com.github.aroq.drupipe.actions

class Terraform extends BaseAction {

    String terraformExecutable = 'terraform'

    def initializeAction() {
        if (action.pipeline.context.jenkinsParams.containsKey('workingDir')) {
            action.params.workingDir = action.pipeline.context.jenkinsParams.workingDir
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
            ${terraformExecutable} init -input=false -backend-config="address=${this.action.pipeline.context.env.TF_VAR_consul_address}" -backend-config="access_token=\${CONSUL_ACCESS_TOKEN}"

            """, this.action.params)
        }
    }

    def state() {
        initializeAction()
        script.drupipeShell("""
            cd ${this.action.params.workingDir}
            /usr/bin/terraform-inventory --list > ${action.params.stateFile}
            """, this.action.params)
        this.script.stash name: 'terraform-state}', includes: "${action.params.stateFile}"
    }

    def executeTerraformCommand(String terraformCommand) {
        String terraformEnv = this.action.pipeline.context.jenkinsParams.terraformEnv
        String terraformWorkspace = this.action.pipeline.context.jenkinsParams.terraformEnv ? this.action.pipeline.context.jenkinsParams.terraformEnv : 'default'

        initializeAction()

        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN'), script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
        script.withCredentials(creds) {
            this.script.drupipeShell("""
            cd ${this.action.params.workingDir}
            TF_WORKSPACE=${terraformWorkspace} TF_VAR_consul_access_token=\$CONSUL_ACCESS_TOKEN ${this.terraformExecutable} ${terraformCommand} -var-file=terraform/${terraformEnv}/terraform.tfvars -var-file=terraform/${terraformEnv}/secrets.tfvars
            """, this.action.params)
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
        String terraformEnv = this.action.pipeline.context.jenkinsParams.terraformEnv
        String terraformWorkspace = this.action.pipeline.context.jenkinsParams.terraformEnv ? this.action.pipeline.context.jenkinsParams.terraformEnv : 'default'

        initializeAction()

        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN'), script.string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
        script.withCredentials(creds) {
            this.script.drupipeShell("""
            cd ${this.action.params.workingDir}
            TF_WORKSPACE=${terraformWorkspace} TF_VAR_consul_access_token=\$CONSUL_ACCESS_TOKEN ${this.terraformExecutable} destroy -force=true -input=false -var-file=terraform/${terraformEnv}/terraform.tfvars -var-file=terraform/${terraformEnv}/secrets.tfvars
            """, this.action.params)
        }
    }

}
