package com.github.aroq.drupipe.actions

import groovy.json.JsonOutput

def deployWithGit(params) {
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    params.ansiblePlaybookParams = [
        target:    params.ansible_target,
        user:      params.ansible_user,
        repo:      params.ansible_repo,
        reference: params.ansible_reference,
        deploy_to: params.ansible_deploy_to,
    ]
    executeAnsiblePlaybook(params)
}

def deployWithAnsistrano(params) {
    drupipeShell("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback", params)

    params.ansiblePlaybookParams = [
        target:                  params.ansible_target,
        user:                    params.ansible_user,
        ansistrano_deploy_via:   params.ansistrano_deploy_via,
        ansistrano_deploy_to:    params.ansible_deploy_to,
        ansistrano_shared_paths: params.ansistrano_shared_paths,
        ansistrano_shared_files: params.ansistrano_shared_files,
    ]

    if (params.ansistrano_deploy_via == 'rsync') {
        drupipeShell("rm -fR docroot/master/.git", params)
        params.ansiblePlaybookParams << [
            ansistrano_deploy_from: params.ansistrano_deploy_from,
        ]
    }
    else if (params.ansistrano_deploy_via == 'git') {
        def version = readFile('docroot/master/VERSION')
        params.ansiblePlaybookParams << [
            ansistrano_git_repo:   params.ansible_repo,
            ansistrano_git_branch: version,
        ]
    }

    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    executeAnsiblePlaybook(params)
    deleteDir()
}

// TODO: Provide Ansible parameters from settings container.
def executeAnsiblePlaybook(params) {
    utils = new com.github.aroq.drupipe.Utils()
    utils.loadLibrary(this, params)
    def command =
        "ansible-playbook ${params.ansible_playbook} \
        -i ${params.ansible_hostsFile} \
        -e '${joinParams(params.ansiblePlaybookParams, 'json')}'"

    echo "Ansible command: ${command}"

    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        drupipeShell("""
            ${command}
            """, params << [shellCommandWithBashLogin: true]
        )
    }
}

@NonCPS
def joinParams(params, mode = 'plain') {
    if (mode == 'plain') {
        params.inject([]) { result, entry ->
            result << "${entry.key}=${entry.value.toString()}"
        }.join(' ')
    }
    else if (mode == 'json') {
        JsonOutput.toJson(params)
    }
}
