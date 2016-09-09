debug = false
docrootDir = 'docroot'
commandParams = [
    JobDslSeed: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
