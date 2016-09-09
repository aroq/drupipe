docrootDir = 'docroot'
commandParams = [
    jobDslSeed : [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
]
