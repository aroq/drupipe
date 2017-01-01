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
	    ]
	]
    ], params)
    params.ansible << [
        playbook: 'library/ansible/deployWithGit.yml',
        reference: 'develop',
        deploy_to: '/var/www/dev'
    ]
    executeAnsiblePlaybook(params)
}

def executeAnsiblePlaybook(params, environmentVariables = [:]) {
    def command =
        "ansible-playbook ${params.ansible.playbook} \
        -i ${params.ansible.hostsFile} \
        -e 'target=${params.ansible.target} \
        user=${params.ansible.user} \
        repo=${params.ansible.repo} \
        reference=${params.ansible.reference} \
        deploy_to=${params.ansible.deploy_to}'"


    echo "Ansible command: ${command}"

    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        sh"""#!/bin/bash -l
            ${command}
        """
    }
}

