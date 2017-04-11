debugEnabled = false
docrootDir = 'docroot'
docmanConfigPath = 'docroot/config'
docmanConfigFile = 'docroot.config'

drupipeLibraryUrl = 'https://github.com/aroq/drupipe.git'
drupipeLibraryBranch = 'master'
drupipeLibraryType = 'branch'
dockerImage = 'aroq/drudock:1.4.0'
nodeName = 'default'
containerMode = 'docker'

// Environments section.
environments {
    dev {
        drupipeLibraryBranch = 'develop'
        dockerImage = 'aroq/drudock:dev'
    }
    stage {
        drupipeLibraryBranch = 'develop'
        dockerImage = 'aroq/drudock:dev'
    }
    prod {
        drupipeLibraryBranch = 'v0.6.3'
        drupipeLibraryType = 'tag'
    }
}

defaultActionParams = [
    // TODO: add params subsections (that will be containerized inside common config).
    Config: [
        //projectConfigPath: 'docroot/config',
        //projectConfigFile: 'docroot.config',
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
    ],
    drupipeWithKubernetes: [
        containerName: 'drudock',
    ],
    Docman: [
        docmanJsonConfigFile: 'config.json',
        build_type: 'git_target',
    ],
    Docman_stripedBuild: [
        build_type: 'striped',
        state: 'stable',
    ],
    Docman_releaseBuild: [
        state: 'stable',
    ],
    // TODO: add private (that will not go into common config) params section.
    Publish_junit: [
        reportsPath: 'reports/*.xml'
    ],
    JobDslSeed_perform: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src'],
        // TODO: Need another way of providing dsl scripts.
        jobsPattern: ['library/jobdsl/seed/*.groovy'],
    ],
    Druflow: [
        druflowDir: 'druflow',
        druflowRepo: 'https://github.com/aroq/druflow.git',
        druflowGitReference: 'v0.1.0',
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
    Druflow_getGitRepo: [
        executeCommand: 'gitGetRepo',
    ],
    Ansible: [
        debugEnabled: true,
        hosts: 'docroot/config/ansible/inventory.ini',
    ],
    Ansible_deployWithGit: [
        playbook: 'library/ansible/deployWithGit.yml',
    ],
    Ansible_deployWithAnsistrano: [
        playbook: 'library/ansible/deployWithAnsistrano.yml',
        playbookParams: [
            ansistrano_deploy_via: 'rsync',
            ansistrano_deploy_from: '../../docroot/master/',
        ],
    ],
    Common_confirm: [
        timeToConfirm: 60,
    ],
    Builder: [
        buildHandler: 'Docman',
        buildMethod: 'build',
        artifactHandler: 'GitArtifact',
    ],
    Deployer: [
        buildHandler: 'Docman',
        artifactHandler: 'GitArtifact',
    ],

    GitArtifact: [
        dir: 'artifacts',
        repoDirName: 'master',
    ],
    Git: [
        singleBranch: true,
        depth: 1,
    ],
]
