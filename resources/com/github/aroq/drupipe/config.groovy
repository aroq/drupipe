debugEnabled = false
docrootDir = 'docroot'
docmanConfigPath = 'docroot/config'
docmanConfigFile = 'docroot.config'
docmanJsonConfigFile = 'config.json'

drupipeLibraryUrl = 'https://github.com/aroq/drupipe.git'
drupipeLibraryBranch = 'master'
drupipeLibraryType = 'branch'
dockerImage = 'aroq/drudock:1.3.0'
nodeName = 'default'

// Environments section.
environments {
    dev {
        drupipeLibraryBranch = 'develop'
        dockerImage = 'aroq/drudock:dev'
    }
    test {
        drupipeLibraryBranch = 'master'
        dockerImage = 'aroq/drudock:latest'
    }
    prod {
        drupipeLibraryBranch = 'v0.3.8'
        drupipeLibraryType = 'tag'
        dockerImage = 'aroq/drudock:1.3.1'
    }
}

actionParams = [
    // TODO: add params subsections (that will be containerized inside common config).
    Config: [
        projectConfigPath: 'docroot/config',
        projectConfigFile: 'docroot.config',
        mothershipConfigFile: 'mothership.config',
    ],
    Behat: [
        masterPath: 'docroot/master',
        masterRelativePath: '..',
        behatExecutable: 'bin/behat',
        pathToEnvironmentConfig: 'code/common',
        workspaceRelativePath: '../../..',
        behat_args: '--format=pretty --out=std --format=junit',
    ],
    drupipeWithDocker: [
        drupipeDockerArgs: '--user root:root',
        noNode: true,
    ],
    Docman: [
        debugEnabled: true,
        docmanJsonConfigFile: 'config.json',
    ],
    // TODO: add private (that will not go into common config) params section.
    Docman_config: [
        docmanJsonConfigFile: 'config.json',
    ],
    Publish_junit: [
        reportsPath: 'reports/*.xml'
    ],
    JobDslSeed_perform: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src'],
        // TODO: Need another way of providing dsl scripts.
        jobsPattern: ['library/jobdsl/job_dsl_docman.groovy'],
    ],
    Nexus: [
        nexusReleaseType: 'release',
    ],
    Druflow: [
        druflowDir: 'druflow',
        druflowRepo: 'https://github.com/aroq/druflow.git',
        druflowGitReference: '0.1.0',
    ],
    Druflow_deployFlow: [
        propertiesFile: 'docroot/master/version.properties',
        executeCommand: 'deployFlow',
    ],
    Druflow_copySite: [
        executeCommand: 'dbCopyAC',
    ],
    Druflow_dbBackupSite: [
        executeCommand: 'dbBackupSite',
    ],
    Ansible: [
        ansible_hostsFile: 'docroot/config/ansible/inventory.ini',
    ],
    Ansible_deployWithGit: [
        ansible_playbook: 'library/ansible/deployWithGit.yml',
    ],
    Ansible_deployWithAnsistrano: [
        ansible_playbook: 'library/ansible/deployWithAnsistrano.yml',
    ],
    Common_confirm: [
        timeToConfirm: 60,
    ],
]
