debugEnabled = false
docrootDir = 'docroot'
actionParams = [
    Docman: [
        debugEnabled: true,
        debugMode: 'json',
    ],
    JobDslSeed: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
