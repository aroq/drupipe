config_version = 1

debugEnabled = false
docrootDir = 'docroot'
docmanDir = 'docman'
projectConfigPath = '.unipipe/config'
projectConfigFile = 'docroot.config'

drupipeLibraryUrl = 'https://github.com/michael-molchanov/drupipe.git'
drupipeLibraryBranch = 'master'
drupipeLibraryType = 'branch'
dockerImage = 'aroq/drudock:1.4.0'
nodeName = 'default'
containerMode = 'docker'

configSeedType = 'docman'

defaultDocmanImage = 'michaeltigr/zebra-build-php-drush-docman:latest'

logRotatorNumToKeep = 5

drupipeDockerArgs = '--user root:root --net=host'

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

params = [
    block: [

    ],
    action: [
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
            additionalClasspath: ['.unipipe/library/src'],
            // TODO: Need another way of providing dsl scripts.
            jobsPattern: ['.unipipe/library/jobdsl/seed/*.groovy'],
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
            playbooksDir: '.unipipe/library/ansible',
            playbook: 'deployWithAnsistrano.yml',
            playbookParams: [
                ansistrano_deploy_via: 'rsync',
            ],
        ],
        Ansible_deployWithGit: [
            playbook: 'deployWithGit.yml',
        ],
        Ansible_deployWithAnsistrano: [
            playbook: 'deployWithAnsistrano.yml',
            playbookParams: [
                ansistrano_deploy_via: 'rsync',
                ansistrano_deploy_from: 'docroot/master',
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
            deployFile: 'unipipe.y*ml',
        ],
        GCloud: [
            executable: 'gcloud',
            kubectl_config_file: '.kubeconfig',
            env: [
                KUBECONFIG: '${context.drupipe_working_dir}/${action.params.kubectl_config_file}'
            ],
            access_key_file_id: '',
            credentials: [
                secret_values_file: [
                    type: 'file',
                    id: '${action.params.access_key_file_id}',
                ],
            ],
            compute_zone: '',
            project_name: '',
            cluster_name: '',
        ],
        // Examples of overriding command with jenkin params:
        // HELM_EXECUTABLE: test
        // HELM_APPLY_EXECUTABLE: test
        // HELM_APPLY_HELM_COMMAND: test
        Helm: [
            executable: 'helm',
            environment: '',
            chart_name: '', // HELM_CHART_NAME in Jenkins params.
            charts_dir: 'charts',
            kubectl_config_file: '.kubeconfig',
            env: [
                KUBECONFIG: '${context.drupipe_working_dir}/${action.params.kubectl_config_file}'
            ],
        ],
        Helm_init: [
            command: 'init',
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
            ],
        ],
        Helm_apply: [
            command: 'upgrade',
            value_suffix: 'values.yaml',
            timeout: '120',
            release_name: '${action.params.chart_name}-${action.params.environment}',
            namespace: '${action.params.chart_name}-${action.params.environment}',
            values_file: '${action.params.chart_name}.${action.params.value_suffix}',
            env_values_file: '${action.params.environment}.${action.params.values_file}',
            secret_values_file_id: '',
            chart_dir: '${action.params.charts_dir}/${action.params.chart_name}',
            credentials: [
                secret_values_file: [
                    type: 'file',
                    id: '${action.params.secret_values_file_id}',
                ],
            ],
            flags: [
                '--install': [''],
                '--wait': [''],
                '--timeout': ['${action.params.timeout}'],
                '--namespace': ['${action.params.namespace}'],
                // TODO: Files are REQUIRED now. Need to add checks in flags processing to make files optional.
                '-f': [
                    '${action.params.values_file}',
                    '${action.params.env_values_file}',
                    '\\\$${action.params.secret_values_file_id}', // To interpolate first "$" sign inside shell script.
                ]
            ],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${prepareFlags(action.params.flags)}',
                '${action.params.release_name}',
                '${action.params.chart_dir}',
            ],
        ],
        Helm_status: [
            command: 'status',
            release_name: '${action.params.chart_name}-${action.params.environment}',
            flags: [:],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${action.params.release_name}',
            ],
        ],
        Helm_delete: [
            command: 'delete',
            release_name: '${action.params.chart_name}-${action.params.environment}',
            flags: [
                '--purge': [''],
            ],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${prepareFlags(action.params.flags)}',
                '${action.params.release_name}',
            ],
        ],
        Kubectl: [
            executable: 'kubectl',
            kubectl_config_file: '.kubeconfig',
            env: [
                KUBECONFIG: '${context.drupipe_working_dir}/${action.params.kubectl_config_file}'
            ],
        ],
        Kubectl_scale: [
            command: 'scale',
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
            ],
        ],
        Kubectl_getPods: [
            command: 'get pods',
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
            ],
        ],
    ],
]
