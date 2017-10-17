debugEnabled = false
docrootDir = 'docroot'
projectConfigPath = 'docroot/config'
projectConfigFile = 'docroot.config'

drupipeLibraryUrl = 'https://github.com/aroq/drupipe.git'
drupipeLibraryBranch = 'master'
drupipeLibraryType = 'branch'
dockerImage = 'aroq/drudock:1.4.0'
nodeName = 'default'
containerMode = 'docker'

configSeedType = 'docman'

logRotatorNumToKeep = 5

// Environments section.
environments {
    dev {
//        dockerImage = 'aroq/drudock:dev'
    }
    stage {
//        dockerImage = 'aroq/drudock:dev'
    }
    prod {
//        dockerImage = 'aroq/drudock:dev'
    }
}

defaultActionParams = [
    // TODO: add params subsections (that will be containerized inside common config).
    Config: [
        //projectConfigPath: 'docroot/config',
        //projectConfigFile: 'docroot.config',
        mothershipConfigFile: 'mothership.config',
        interpolate: 0,
    ],
    Source: [
        interpolate: 0,
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
        drupipeDockerArgs: '--user root:root --net=host',
    ],
    drupipeWithKubernetes: [
        containerName: 'drudock',
    ],
    Terraform: [
        infraSourceName: 'infra-config',
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
    Gitlab_acceptMR: [
        message: 'MR merged as pipeline was executed successfully.',
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
        druflowGitReference: 'v0.1.3',
    ],
    Druflow_operations: [
        propertiesFile: 'docroot/master/version.properties',
        executeCommand: 'deployFlow',
    ],
    Druflow_deploy: [
        propertiesFile: 'docroot/master/version.properties',
        executeCommand: 'deployTag',
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
        //hosts: 'docroot/config/ansible/inventory.ini',
        playbook: 'library/ansible/deployWithAnsistrano.yml',
        playbookParams: [
            ansistrano_deploy_via: 'rsync',
        ],
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
    PipelineController: [
        buildHandler: [
            method: 'build',
        ],
        deployHandler: [
            method: 'deploy',
        ],
        artifactHandler: [
            handler: 'GitArtifact',
            method: 'retrieve',
        ],
        operationsHandler: [
            method: 'operations',
        ],
    ],
    GitArtifact: [
        dir: 'artifacts',
        repoDirName: 'master',
    ],
    Git: [
        singleBranch: true,
        depth: 1,
    ],
    YamlFileHandler: [
        deployFile: '.drupipe.yml',
    ],
    Helm: [
        executable:   'helm',
        charts_dir:   'charts',
        kubectl_config_file: '.kubeconfig',
        working_dir: '',
    ],
    // HELM_EXECUTABLE: test
    // HELM_APPLY_EXECUTABLE: test
    // HELM_APPLY_HELM_COMMAND: test
    Helm_apply: [
        command: 'upgrade',
        value_suffix: 'values.yaml',
        chart_name: '',
        env: '',
        timeout: '120',
        release_name: '${action.params.chart_name}-${action.params.env}',
        namespace: '${action.params.chart_name}-${action.params.env}',
        values_file: '${action.params.chart_name}.${action.params.value_suffix}',
        env_values_file: '${action.params.env}.${action.params.values_file}',
        secret_values_file_id: '',
        chart_dir: '${action.params.charts_dir}/${action.params.chart_name}',
        flags: [
            '--install': [''],
            '--wait': [''],
            '--timeout': ['${action.params.timeout}'],
            '--namespace': ['${action.params.namespace}'],
            '-f': [
                '${action.params.values_file}',
                '${action.params.env_values_file}',
                '\\$${action.params.secret_values_file_id}', // To interpolate inside shell script.
            ]
        ]
    ],
    Helm_status: [
        command: 'status',
        flags: [:]
    ],
    Helm_delete: [
        command: 'delete',
        flags: [
            '--purge': [''],
        ]
    ],

]
