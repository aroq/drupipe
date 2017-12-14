package com.github.aroq.drupipe.actions

import groovy.json.JsonOutput

class Ansible extends BaseAction {

    def init() {
        if (!action.params.playbookParams) {
            action.params.playbookParams = [:]
        }
        action.params.playbookParams <<  [
            user: action.pipeline.context.environmentParams.user,
            drupipe_environment: action.pipeline.context.environment,
        ]
        if (!action.params.inventoryArgument) {
            if (action.params.inventory && action.pipeline.context.environmentParams.default_group) {
                action.params.inventoryArgument = action.params.inventory.path
                action.params.playbookParams.target = "${action.pipeline.context.environmentParams.default_group}"
            }
            else {
                action.params.inventoryArgument = "${action.pipeline.context.environmentParams.host},"
                action.params.playbookParams.target = "${action.pipeline.context.environmentParams.host}"
            }
        }
    }

    def deploy() {
        init()
        script.echo(action.pipeline.context.builder.artifactParams.dir)
        def relativePath = utils.getRelativePath(action.pipeline.context, action.params.playbooksDir, action.pipeline.context.builder.artifactParams.dir)
        action.params.playbookParams << [
            ansistrano_deploy_to:   action.pipeline.context.environmentParams.root,
            ansistrano_deploy_from: relativePath + '/',
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
            repo:      action.pipeline.context.builder.artifactParams.repoAddress,
            reference: action.pipeline.context.builder.artifactParams.reference,
            deploy_to: action.pipeline.context.environmentParams.root,
        ]
        executeAnsiblePlaybook()
    }

    def installAnsistranoRole() {
        // TODO: Install role in docker image.
        script.drupipeShell("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback", action.params)
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
        if (action.pipeline.context.jenkinsParams.containsKey('workingDir')) {
            action.params.workingDir = action.pipeline.context.jenkinsParams.workingDir
        }
        else {
            action.params.workingDir = '.'
        }
        action.pipeline.scripts_library_load()

        // TODO: Make it centralized.
        String vaultPassFile
        if (action.pipeline.context.containerMode == 'kubernetes') {
            this.script.drupipeShell("""
            echo "\${ANSIBLE_VAULT_PASS_FILE}" > .vault_pass
            """, this.action.params
            )
            vaultPassFile = action.params.workingDir != '.' ? '../.vault_pass' : '.vault_pass'
        }
        else {
            vaultPassFile = "\${ANSIBLE_VAULT_PASS_FILE}"
        }

        def command =
            """pwd && ls -lah && ansible-playbook ${action.params.playbooksDir}/${action.params.playbook} \
            -i ${action.params.inventoryArgument} \
            --vault-password-file ${vaultPassFile} \
            -e '${joinParams(action.params.playbookParams, 'json')}'"""

        script.echo "Ansible command: ${command}"

        def creds = [script.file(credentialsId: 'ANSIBLE_VAULT_PASS_FILE', variable: 'ANSIBLE_VAULT_PASS_FILE')]

        script.withCredentials(creds) {
            this.script.drupipeShell("""
                cd ${this.action.params.workingDir}
                ${command}
            """, this.action.params
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
