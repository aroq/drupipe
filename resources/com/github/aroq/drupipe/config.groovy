config_version = 1

// Default params.
environment = ''
debugEnabled = false
docrootDir = 'docroot'
projectConfigPath = 'docroot/config'
projectConfigFile = 'docroot.config'


dockerImage = 'aroq/drudock:1.4.0'
nodeName = 'default'
containerMode = 'docker'
configSeedType = 'docman'
defaultDocmanImage = 'michaeltigr/zebra-build-php-drush-docman:latest'
logRotatorNumToKeep = 5
drupipeDockerArgs = '--user root:root --net=host'

params = [
    pipeline: [
        scripts_library: [
            url: 'https://github.com/aroq/drupipe.git',
            ref: 'master',
            type: 'branch',
        ]
    ],
    block: [

    ],
    action: [
        // Default action params (merged to all actions params).
        ACTION: [
            action_timeout: 120,
            store_result: true,
            dump_result: true,
            store_action_params: true,
            store_result_key: 'context.results.action.${action.name}_${action.methodName}',
            post_process: [
                result: [
                    type: 'result',
                    source: 'result',
                    destination: '${action.params.store_result_key}',
                ],
            ],
            store_action_params_key: 'actions.${action.name}_${action.methodName}',
            shell_bash_login: true,
            return_stdout: false,
        ],
        // TODO: add params subsections (that will be containerized inside common config).
        Config: [
            post_process: [
                context: [
                    type: 'result',
                    source: 'result',
                    destination: 'context',
                ],
            ],
        ],
        Config_perform: [
            post_process: [
            ],
        ],
        Config_envConfig: [
        ],
        Config_mothershipConfig: [
            mothershipConfigFile: 'mothership.config',
            post_process: [
                result: [
                    type: 'result',
                    source: 'result.configRepo',
                    destination: 'context.configRepo',
                ],
            ],
        ],
        Config_projectConfig: [
        ],
        Source: [
            post_process: [
                context: [
                    type: 'result',
                    source: 'result',
                    destination: 'context',
                ],
            ],
        ],
        YamlFileConfig: [
        ],
        GroovyFileConfig: [
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
            shell_bash_login: false,
        ],
        DrushFeaturesList: [
            return_stdout: true,
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
            deployFile: 'unipipe.y*ml',
        ],
        GCloud: [
            executable: 'gcloud',
            kubectl_config_file: '.kubeconfig',
            env: [
                KUBECONFIG: '${context.drupipe_working_dir}/${action.params.kubectl_config_file}'
            ],
            access_key_file_id: '',
            shell_bash_login: false,
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
        // Examples of overriding command with jenkins params:
        // HELM_EXECUTABLE: test
        // HELM_APPLY_EXECUTABLE: test
        // HELM_APPLY_HELM_COMMAND: test
        Jenkins: [
            shell_bash_login: false,
        ],
        Helm: [
            executable: 'helm',
            chart_name: '', // HELM_CHART_NAME in Jenkins params.
            release_name: '${action.params.chart_name}-${context.environment}',
            charts_dir: 'charts',
            kubectl_config_file: '.kubeconfig',
            shell_bash_login: false,
            namespace: '${action.params.chart_name}-${context.environment}',
            env: [
                KUBECONFIG: '${context.drupipe_working_dir}/${action.params.kubectl_config_file}'
            ],
            post_process: [
                namespace: [
                    type: 'result',
                    source: 'params.namespace', // From action params.
                    destination: 'context.k8s.namespace',  // To "context".
                ],
                release_name: [
                    type: 'result',
                    source: 'params.release_name', // From action params.
                    destination: 'context.k8s.selector',  // To "context".
                ],
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
            values_file: '${action.params.chart_name}.${action.params.value_suffix}',
            env_values_file: '${context.environment}.${action.params.values_file}',
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
            flags: [:],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${action.params.release_name}',
            ],
        ],
        Helm_delete: [
            command: 'delete',
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
            shell_bash_login: false,
            namespace: '${context.k8s.namespace}',
            env: [
                KUBECONFIG: '${context.drupipe_working_dir}/${action.params.kubectl_config_file}'
            ],
        ],
        Kubectl_scale_replicaset: [
            command: 'scale replicaset',
            replicas: '',
            name: '',
            flags: [
                '--replicas': ['${action.params.replicas}'],
                '--namespace': ['${action.params.namespace}'],
            ],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${prepareFlags(action.params.flags)}',
                '${action.params.name}',
            ],
        ],
        Kubectl_scale_down_up: [
            replicas_down: '0',
            replicas_up: '1',
        ],
        Kubectl_get_replicaset_name: [
            command: 'get replicaset',
            release_name: '${actions.Helm_status.release_name}',
            jsonpath: '\'{.items[0].metadata.name}\'',
            return_stdout: true,
            selector: 'release=${context.k8s.selector}',
            flags: [
                '--namespace': ['${action.params.namespace}'],
                '--selector': ['${action.params.selector}'],
                '-o': ['jsonpath=${action.params.jsonpath}'],
            ],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${prepareFlags(action.params.flags)}',
            ],
        ],
        Kubectl_get_pod_name: [
            command: 'get pod',
            release_name: '${actions.Helm_status.release_name}',
            jsonpath: '\'{.items[0].metadata.name}\'',
            return_stdout: true,
            selector: 'release=${context.k8s.selector}',
            flags: [
                '--namespace': ['${action.params.namespace}'],
                '--selector': ['${action.params.selector}'],
                '-o': ['jsonpath=${action.params.jsonpath}'],
            ],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${prepareFlags(action.params.flags)}',
            ],
        ],
        Kubectl_get_pods: [
            command: 'get pods',
            flags: [
                '--namespace': ['${action.params.namespace}'],
            ],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${prepareFlags(action.params.flags)}',
            ],
        ],
        Kubectl_get_loadbalancer_address: [
            command: 'get service',
            release_name: '${actions.Helm_status.release_name}',
            jsonpath: '\'{.items[0].status.loadBalancer.ingress[0].ip}:{.items[0].spec.ports[?(@.name=="http")].port}\'',
            return_stdout: true,
            selector: 'release=${context.k8s.selector}',
            flags: [
                '--namespace': ['${action.params.namespace}'],
                '--selector': ['${action.params.selector}'],
                '-o': ['jsonpath=${action.params.jsonpath}'],
            ],
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${prepareFlags(action.params.flags)}',
            ],
        ],
        Kubectl_copy_from_pod: [
            command: 'cp',
            name: '',
            source_file_name: '',
            source: '${context.k8s.namespace}/${action.params.name}:${action.params.source_file_name}',
            destination: '',
            full_command: [
                '${action.params.executable}',
                '${action.params.command}',
                '${action.params.source}',
                '${action.params.destination}',
            ],
        ],
        HealthCheck_wait_http_ok: [
            action_timeout: 5,
            url: '',
            http_code: '200',
            interval: '5',
            command: '''bash -c 'while [[ "\\\$(curl -s -o /dev/null -w ''%{http_code}'' ${action.params.url})" != "${action.params.http_code}" ]]; do sleep ${action.params.interval}; done' ''',
            full_command: [
                '${action.params.command}',
            ],
        ],
    ],
]
