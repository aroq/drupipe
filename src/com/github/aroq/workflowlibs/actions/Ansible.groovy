package com.github.aroq.workflowlibs.actions

def deployWithGit(params) {
    executePipelineAction([
	action: 'Source.add',
	params: [
	    source: [
		ansible_name: 'library',
		ansible_type: 'git',
		ansible_path: 'library',
		ansible_url: params.drupipeLibraryUrl,
		ansible_branch: params.drupipeLibraryBranch,
	    ]
	]
    ], params)
    // params.ansible << [:]
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

