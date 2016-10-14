debugEnabled = true
docrootDir = 'docroot'
actionParams = [
    // TODO: add params subsections (that will be containerized inside common config).
    Config: [
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
//        debugEnabled: true,
    ],
    Docman_jsonConfig: [
        docmanConfigPath: 'docroot/config',
        docmanConfigFile: 'docroot.config',
        docmanJsonConfigFile: 'config.json',
//        debugEnabled: true,
    ],
    Docman_info: [
//        debugEnabled: true,
    ],
    Script_execute: [
    ],
    Publish_junit: [
        reportsPath: 'reports/*.xml'
    ],
    JobDslSeed: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ],
    Druflow_deployFlow: [
        propertiesFile: 'docroot/master/version.properties'
    ],
]
