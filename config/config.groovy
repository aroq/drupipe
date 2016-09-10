debugEnabled = false
docrootDir = 'docroot'
actionParams = [
    Config: [
        returnConfig: true
    ],
    DocmanConfig: [
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
