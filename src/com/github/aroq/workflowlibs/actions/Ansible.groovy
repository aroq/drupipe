package com.github.aroq.workflowlibs.actions

def deployWithGit(params) {
    params.ansible = [
        playbook: 'library/ansible/deployWithGit.yml',
        reference: 'develop',
        deploy_to: '/var/www/dev'
    ]
    executeAnsiblePlaybook(params)
}

def executeAnsiblePlaybook(params, environmentVariables = [:]) {
    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        sh"""#!/bin/bash -l
        ansible-playbook ${params.ansible.playbook} \
        -i ${params.ansible.hostsFile} \
        -e 'target=${params.ansible.target} \
        user=${params.ansible.user} \
        repo=${params.ansible.repo} \
        reference=${params.ansible.reference} \
        deploy_to=${params.ansible.deploy_to}'
        """
    }
}

