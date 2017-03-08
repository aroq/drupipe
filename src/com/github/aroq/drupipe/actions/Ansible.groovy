package com.github.aroq.drupipe.actions

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
    // TODO: refactor it.
    drupipeShell("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback", params)
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    def version = readFile('docroot/master/VERSION')
    params.ansiblePlaybookParams = [
        target:                 params.ansible_target,
        user:                   params.ansible_user,
        ansible_reference:      version,
        ansistrano_deploy_from: '../../docroot/master',
        ansistrano_git_repo:    params.ansible_repo,
        ansistrano_git_branch:  params.ansible_reference,
        ansistrano_deploy_via:  params.ansistrano_deploy_via,
        ansistrano_deploy_to:   params.ansible_deploy_to,
    ]
    executeAnsiblePlaybook(params)
}

// TODO: Provide Ansible parameters from settings container.
def executeAnsiblePlaybook(params, environmentVariables = [:]) {
    utils = new com.github.aroq.drupipe.Utils()
    utils.loadLibrary(this, params)
    def command =
        "ansible-playbook ${params.ansible_playbook} \
        -i ${params.ansible_hostsFile} \
        -e '${joinParams(params.ansiblePlaybookParams)}'"

    echo "Ansible command: ${command}"

    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        drupipeShell("""
            ${command}
            """, params << [shellCommandWithBashLogin: true]
        )
    }
}

@NonCPS
def joinParams(params) {
    params.inject([]) { result, entry ->
        result << "${entry.key}=${entry.value.toString()}"
    }.join(' ')
}


