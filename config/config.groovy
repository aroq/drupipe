debugEnabled = false
docrootDir = 'docroot'
actionParams = [
    Config: [
    ],
    DocmanConfig: [
    ],
    Source: [
    ],
    GroovyFileConfig: [
    ],
    Docman: [
//        debugEnabled: true,
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
