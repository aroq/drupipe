debugEnabled = false
docrootDir = 'docroot'
actionParams = [
    Source: [
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
