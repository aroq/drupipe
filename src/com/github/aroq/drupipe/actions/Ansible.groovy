package com.github.aroq.drupipe.actions

import groovy.json.JsonOutput

class Builder extends BaseAction {
    def deployWithGit() {
        // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
        action.params.ansiblePlaybookParams = [
            target:    action.params.ansible_target,
            user:      action.params.ansible_user,
            repo:      action.params.ansible_repo,
            reference: action.params.ansible_reference,
            deploy_to: action.params.ansible_deploy_to,
        ]
        executeAnsiblePlaybook()
    }

    def installAnsistranoRole() {
        script.drupipeShell("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback", context)
    }

    def deployWithAnsistrano() {
        installAnsistranoRole()

        action.params.ansiblePlaybookParams = [
            target:                  action.params.ansible_target,
            user:                    action.params.ansible_user,
            ansistrano_deploy_via:   action.params.ansistrano_deploy_via,
            ansistrano_deploy_to:    action.params.ansible_deploy_to,
            ansistrano_shared_paths: action.params.ansistrano_shared_paths,
            ansistrano_shared_files: action.params.ansistrano_shared_files,
        ]

        if (action.params.ansistrano_deploy_via == 'rsync') {
            script.drupipeShell("rm -fR docroot/master/.git", context)
            action.params.ansiblePlaybookParams << [
                ansistrano_deploy_from: action.params.ansistrano_deploy_from,
            ]
        }
        else if (action.params.ansistrano_deploy_via == 'git') {
            def version = readFile('docroot/master/VERSION')
            action.params.ansiblePlaybookParams << [
                ansistrano_git_repo:   params.ansible_repo,
                ansistrano_git_branch: version,
            ]
        }

        // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
        executeAnsiblePlaybook()
        script.deleteDir()
    }

    // TODO: Provide Ansible parameters from settings container.
    def executeAnsiblePlaybook() {
        utils.loadLibrary(script, context)
        def command =
            "ansible-playbook ${action.params.ansible_playbook} \
        -i ${action.params.ansible_hostsFile} \
        -e '${joinParams(action.params.ansiblePlaybookParams, 'json')}'"

        script.echo "Ansible command: ${command}"

        script.drupipeShell("""
            ${command}
            """, context << [shellCommandWithBashLogin: true]
        )
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
