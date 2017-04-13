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
        ]
        action.params.hosts = "${context.environmentParams.host},"
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
        // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
        action.params.playbookParams = [
            target:    action.params.ansible_target,
            user:      action.params.ansible_user,
            repo:      action.params.ansible_repo,
            reference: action.params.ansible_reference,
            deploy_to: action.params.ansible_deploy_to,
        ]
        executeAnsiblePlaybook()
    }

    def installAnsistranoRole() {
        // TODO: Install role in docker image.
        script.drupipeShell("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback", context)
    }

    def deployWithAnsistrano() {
        installAnsistranoRole()

        // TODO: do it outside of this method.
//        if (action.params.playbookParams.ansistrano_deploy_via == 'rsync') {
//            script.drupipeShell("rm -fR docroot/master/.git", context)
//        }

        if (action.params.playbookParams.ansistrano_deploy_via == 'git') {
            def version = readFile('docroot/master/VERSION')
            action.params.playbookParams << [
                ansistrano_git_repo:   params.ansible_repo,
                ansistrano_git_branch: version,
            ]
        }

        // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
        executeAnsiblePlaybook()
        // TODO: Move delete dir to somewhere else.
//        if (action.params.deleteDir) {
//            script.drupipeShell("""
//                rm -fR docroot/master
//                """, context
//            )
//        }
    }

    // TODO: Provide Ansible parameters from settings container.
    def executeAnsiblePlaybook() {
        utils.loadLibrary(script, context)
        def command =
            "ansible-playbook ${action.params.playbook} \
        -i ${action.params.hosts} \
        -e '${joinParams(action.params.playbookParams, 'json')}'"

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
