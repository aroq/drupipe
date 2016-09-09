debug = false
docrootDir = 'docroot'
commandParams = [
    JobDslSeed: [
        debug: true,
        debugMode: 'json',
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
