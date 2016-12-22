debugEnabled = false
docrootDir = 'docroot'
jobsPattern = 'library/jobdsl/job_dsl_docman.groovy'

actionParams = [
    // TODO: add params subsections (that will be containerized inside common config).
    Config: [
        projectConfigPath: 'docroot/config',
        projectConfigFile: 'docroot.config',
    ],
    Behat: [
        masterPath: 'docroot/master',
        masterRelativePath: '..',
        behatExecutable: 'bin/behat',
        pathToEnvironmentConfig: 'code/common',
        workspaceRelativePath: '../../..',
        behat_args: '--format=pretty --out=std --format=junit',
    ],
    withDrupipeDocker: [
        drupipeDockerImageName: 'aroq/drudock:1.2.0',
        drupipeDockerArgs: '--user root:root',
        noNode: true,
    ],
    // TODO: add private (that will not go into common config) params section.
    Source_loadConfig: [
    ],
    GroovyFileConfig: [
    ],
    Docman_config: [
        docmanConfigPath: 'docroot/config',
        docmanConfigFile: 'docroot.config',
        docmanJsonConfigFile: 'config.json',
    ],
    Docman_jsonConfig: [
        docmanConfigPath: 'docroot/config',
        docmanConfigFile: 'docroot.config',
        docmanJsonConfigFile: 'config.json',
    ],
    Docman_info: [
    ],
    Docman_deploy: [
        debugEnabled: true,
    ],
    Script_execute: [
    ],
    Publish_junit: [
        reportsPath: 'reports/*.xml'
    ],
    JobDslSeed_perform: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src'],
        debugEnabled: true,
        // TODO: Need another way of providing dsl scripts.
        jobsPattern: 'library/jobdsl/job_dsl_docman.groovy',
    ],
    Druflow: [
        druflowDir: 'druflow',
        druflowRepo: 'https://github.com/aroq/druflow.git',
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
    Common_confirm: [
        timeToConfirm: 60,
    ],
]
