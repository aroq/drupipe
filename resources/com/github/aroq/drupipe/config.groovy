debugEnabled = false
docrootDir = 'docroot'
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
        debugEnabled: true,
    ],
    withDrupipeDocker: [
        drupipeDockerImageName: 'aroq/drudock:1.1.0',
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
    ],
    Druflow_deployFlow: [
        propertiesFile: 'docroot/master/version.properties',
        executeCommand: 'deployFlow'
    ],
]
