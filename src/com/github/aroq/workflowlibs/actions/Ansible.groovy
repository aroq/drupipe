package com.github.aroq.workflowlibs.actions

def deployWithGit(params) {
    executePipelineAction([
        action: 'Source.add',
        params: [
            source: [
                name: 'library',
                type: 'git',
                path: 'library',
                url: params.drupipeLibraryUrl,
                branch: params.drupipeLibraryBranch,
            ],
        ],
    ], params)
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    // params.ansible << [:]
    executeAnsiblePlaybook(params)
}

def deployWithAnsistrano(params) {
    sh("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback")
    def version = readFile('docroot/master/VERSION')
    executePipelineAction([
        action: 'Source.add',
        params: [
            source: [
                name: 'library',
                type: 'git',
                path: 'library',
                url: params.drupipeLibraryUrl,
                branch: params.drupipeLibraryBranch,
            ],
        ],
    ], params)
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    params << [ansible_reference: version]
    executeAnsiblePlaybook(params)
}


def executeAnsiblePlaybook(params, environmentVariables = [:]) {
    def command =
        "ansible-playbook ${params.ansible_playbook} \
        -i ${params.ansible_hostsFile} \
        -e 'target=${params.ansible_target} \
        user=${params.ansible_user} \
        repo=${params.ansible_repo} \
        reference=${params.ansible_reference} \
        deploy_to=${params.ansible_deploy_to}'"

    echo "Ansible command: ${command}"

    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        sh"""#!/bin/bash -l
            ${command}
        """
    }
}

