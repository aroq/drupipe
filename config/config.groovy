debug = false
docrootDir = 'docroot'
actionParams = [
    Source: [
        debug: true,
//        debugMode: 'json',
    ],
    JobDslSeed: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
