package com.github.aroq.drupipe.actions

class Jenkins extends BaseAction {

    def getJenkinsAddress() {
        String terraformEnv = this.action.pipeline.context.jenkinsParams.terraformEnv
        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN')]
        def result = script.withCredentials(creds) {
            this.script.drupipeShell("""
                curl http://\${TF_VAR_consul_address}/v1/kv/zebra/jenkins/${terraformEnv}/address?raw&token=\${CONSUL_ACCESS_TOKEN}
            """, this.action.pipeline.context.clone() << [return_stdout: true])
        }
        result.stdout
    }

    def getJenkinsSlaveAddress() {
        String terraformEnv = this.action.pipeline.context.jenkinsParams.terraformEnv
        def creds = [script.string(credentialsId: 'CONSUL_ACCESS_TOKEN', variable: 'CONSUL_ACCESS_TOKEN')]
        def result = script.withCredentials(creds) {
            this.script.drupipeShell("""
                curl http://\${TF_VAR_consul_address}/v1/kv/zebra/jenkins/${terraformEnv}/slave/address?raw&token=\${CONSUL_ACCESS_TOKEN}
            """, this.action.pipeline.context.clone() << [return_stdout: true])
        }
        result.stdout
    }


    def executeAnsiblePlaybook() {
        // TODO: refactor terraformEnv params into common env param.
        action.params.playbookParams << [
            env: action.pipeline.context.jenkinsParams.terraformEnv,
            jenkins_default_slave_address: getJenkinsSlaveAddress(),
            user_token_temp_file_dest: action.pipeline.context.workspace,
        ]
        action.params.inventoryArgument = getJenkinsAddress() + ','
        script.drupipeAction([action: 'Ansible.executeAnsiblePlaybook', params: [action.params]], action.pipeline.context)
        action.pipeline.context.jenkins_user_token = script.readFile(file: "user_token")
        action.pipeline.context
    }

    def cli(cli_command) {
        // TODO: Remove it after action refactor and tests.
        if (action.params.jenkins_user_token_file) {
            action.params.jenkins_user_token = script.readFile file: action.params.jenkins_user_token_file
            script.echo "jenkins_user_token_file is set action.params.jenkins_user_token to ${action.params.jenkins_user_token}"
        }
        if (!action.params.jenkins_address) {
            action.params.jenkins_address = "${getJenkinsAddress()}:${this.action.params.port}"
        }
        if (action.params.jenkins_user_token) {
            def envvars = ["JENKINS_URL=http://${action.params.jenkins_address}", "JENKINS_API_TOKEN=${action.params.jenkins_user_token}"]
            this.script.withEnv(envvars) {
                this.script.drupipeShell(
"""
JENKINS_URL=http://${this.action.params.jenkins_address} /jenkins-cli/jenkins-cli-wrapper.sh -auth ${this.action.params.user}:${this.action.params.jenkins_user_token} ${cli_command}
""", this.action.params)
            }
        }
        else {
            def creds = [this.script.string(credentialsId: 'JENKINS_API_TOKEN', variable: 'JENKINS_API_TOKEN')]
            this.script.withCredentials(creds) {
                def envvars = ["JENKINS_URL=http://${getJenkinsAddress()}:${this.action.params.port}"]
                this.script.withEnv(envvars) {
                    this.script.drupipeShell(
"""
/jenkins-cli/jenkins-cli-wrapper.sh -auth ${this.action.params.user}:\${JENKINS_API_TOKEN} ${cli_command}
""", this.action.params)
                }
            }
        }
    }

    def build() {
        cli("build ${this.action.params.args} ${this.action.params.jobName}")
    }

    def buildPrepare(String jobName) {
        return {
            cli("build ${this.action.params.args} ${jobName}")
        }
    }

    def seedTest() {
        def mothershipConfig = action.pipeline.drupipeConfig.projects
        def projects = parseProjects(mothershipConfig, 'tests', 'seed').tokenize(',')

        def builds = [:]
        for (def i = 0; i < projects.size(); i++) {
            this.script.echo projects[i]
            builds[i] = buildPrepare("${projects[i]}/seed")
        }
        script.parallel builds
        [:]
    }

    @NonCPS
    def parseProjects(def projects, String param, String tag) {
        def result = []
        for (project in projects) {
            if (project.value?.params?.containsKey(param) && project.value.params[param].contains(tag) ) {
                result << project.key
            }
        }
        result.join(',')
    }

}
