package com.github.aroq.drupipe.actions

def deployWithGit(params) {
    utils = new com.github.aroq.drupipe.Utils()
    utils.loadLibrary(this, params)
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    // params.ansible << [:]
    executeAnsiblePlaybook(params)
}

def deployWithAnsistrano(params) {
    // TODO: refactor it.
    drupipeShell("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback", params)
    utils = new com.github.aroq.drupipe.Utils()
    utils.loadLibrary(this, params)
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    def version = readFile('docroot/master/VERSION')
    params << [
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
    def command =
        "ansible-playbook ${params.ansible_playbook} \
        -i ${params.ansible_hostsFile} \
        -e 'target=${params.ansible_target} \
        user=${params.ansible_user} \
        ansistrano_git_repo=${params.ansible_repo} \
        ansistrano_git_branch=${params.ansible_reference} \
        ansistrano_deploy_from=${params.ansible_deploy_from} \
        ansistrano_deploy_via=${params.ansistrano_deploy_via} \
        ansistrano_deploy_to=${params.ansible_deploy_to}'"

    echo "Ansible command: ${command}"

    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        drupipeShell("""
            ${command}
            """, params << [shellCommandWithBashLogin: true]
        )
    }
}


