debugEnabled = false
docrootDir = 'docroot'
actionParams = [
    Config: [
        debugEnabled: true,
        returnConfig: true
    ],
    DocmanConfig: [
        debugEnabled: true,
        returnConfig: true
    ],
    Source: [
        returnConfig: true
    ],
    Docman: [
        debugEnabled: true,
//        debugMode: 'json',
    ],
    JobDslSeed: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
