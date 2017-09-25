package com.github.aroq.drupipe.actions

import groovy.json.JsonOutput
import com.github.aroq.drupipe.DrupipeAction

class Ansible extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def init() {
        if (!action.params.playbookParams) {
            action.params.playbookParams = [:]
        }
        action.params.playbookParams <<  [
            user: context.environmentParams.user,
            drupipe_environment: context.environment,
        ]
        if (!action.params.inventoryArgument) {
            if (action.params.inventory && context.environmentParams.default_group) {
                action.params.inventoryArgument = action.params.inventory.path
                action.params.playbookParams.target = "${context.environmentParams.default_group}"
            }
            else {
                action.params.inventoryArgument = "${context.environmentParams.host},"
                action.params.playbookParams.target = "${context.environmentParams.host}"
            }
        }
    }

    def deploy() {
        init()
        action.params.playbookParams << [
            ansistrano_deploy_to:   context.environmentParams.root,
            ansistrano_deploy_from: context.builder.artifactParams.dir + '/',
        ]
        deployWithAnsistrano()
    }

    def execute() {
        init()
        executeAnsiblePlaybook()
    }

    def deployWithGit() {
        init()
        action.params.playbookParams = [
            repo:      context.builder.artifactParams.repoAddress,
            reference: context.builder.artifactParams.reference,
            deploy_to: context.environmentParams.root,
        ]
        executeAnsiblePlaybook()
    }

    def installAnsistranoRole() {
        // TODO: Install role in docker image.
        script.drupipeShell("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback", context)
    }

    def deployWithAnsistrano() {
        installAnsistranoRole()

//        if (action.params.playbookParams.ansistrano_deploy_via == 'git') {
//            def version = readFile('docroot/master/VERSION')
//            action.params.playbookParams << [
//                ansistrano_git_repo:   params.ansible_repo,
//                ansistrano_git_branch: version,
//            ]
//        }

        executeAnsiblePlaybook()
    }

    // TODO: Provide Ansible parameters from settings container.
    def executeAnsiblePlaybook() {
        // TODO: move workingDir logic into Config action and use it globally in sh scripts.
        if (context.jenkinsParams.containsKey('workingDir')) {
            action.params.workingDir = context.jenkinsParams.workingDir
        }
        else {
            action.params.workingDir = '.'
        }
        utils.loadLibrary(script, context)

        String vaultPassFile

        if (context.containerMode == 'kubernetes') {
            this.script.drupipeShell("""
            echo "\${ANSIBLE_VAULT_PASS_FILE}" > .vault_pass
            """, this.context << [shellCommandWithBashLogin: true]
            )
            vaultPassFile = '../.vault_pass'
        }
        else {
            vaultPassFile = "\${ANSIBLE_VAULT_PASS_FILE}"
        }

        def command =
            "ansible-playbook ${action.params.playbook} \
            -i ${action.params.inventoryArgument} \
            --vault-password-file ${vaultPassFile} \
            -e '${joinParams(action.params.playbookParams, 'json')}'"

        script.echo "Ansible command: ${command}"

        def creds = [script.file(credentialsId: 'ANSIBLE_VAULT_PASS_FILE', variable: 'ANSIBLE_VAULT_PASS_FILE')]

        script.withCredentials(creds) {
            this.script.drupipeShell("""
                cd ${this.action.params.workingDir}
                ${command}
            """, this.context << [shellCommandWithBashLogin: true]
            )
        }

    }

    @NonCPS
    def joinParams(params, mode = 'plain') {
        params = params.findAll{k, v -> v != null}
        if (mode == 'plain') {
            params.inject([]) { result, entry ->
                result << "${entry.key}=${entry.value.toString()}"
            }.join(' ')
        }
        else if (mode == 'json') {
            JsonOutput.toJson(params)
        }
    }
}
