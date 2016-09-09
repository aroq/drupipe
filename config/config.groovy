debug = false
docrootDir = 'docroot'
commandParams = [
    JobDslSeed: [
        debug: true,
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
