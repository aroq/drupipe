debugEnabled = false
docrootDir = 'docroot'
actionParams = [
    // TODO: add params subsections (that will be containerized inside common config).
    Config: [
    ],
    // TODO: add private (that will not go into common config) params section.
    Source_loadConfig: [
//        debugEnabled: true,
    ],
    GroovyFileConfig: [
    ],
    Docman: [
//        debugMode: 'json',
    ],
    // Dot is not allowed as key name, so used underscore here.
    Docman_info: [
        debugEnabled: true,
    ],
    JobDslSeed: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
