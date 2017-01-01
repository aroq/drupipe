package com.github.aroq.workflowlibs.actions

def deployWithGit(params) {
    params.ansible = [
        playbook: 'docroot/config/ansible/delivery.yml',
        hostsFile: 'docroot/config/ansible/inventory.ini',
        target: 'demo',
        user: 'zebra',
        repo: 'aroq@svn-2625.devcloud.hosting.acquia.com:aroq.git',
        reference: 'develop',
        deploy_to: '/var/www/dev'
    ]
    executeAnsiblePlaybook(params)
}

def executeAnsiblePlaybook(params) {
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

