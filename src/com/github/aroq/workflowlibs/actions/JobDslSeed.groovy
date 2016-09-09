package com.github.aroq.workflowlibs.actions

def perform(params) {
    defaultParams = [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src']
    ]
    testParams = params.commandParams[params.action.name]
    dump(testParams, 'Test params')
    params << defaultParams << params

    jobDsl targets: [params.jobsPattern].join('\n'),
            removedJobAction: params.removedJobAction,
            removedViewAction: params.removedViewAction,
            lookupStrategy: params.lookupStrategy,
            additionalClasspath: params.additionalClasspath.join('\n')
}

