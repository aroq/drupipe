package com.github.aroq.workflowlibs.actions

def perform(params) {
//    defaultParams = [
//        removedJobAction: 'DELETE',
//        removedViewAction: 'DELETE',
//        lookupStrategy: 'SEED_JOB',
//        additionalClasspath: ['library/src']
//    ]
    defaultParams = params.commandParams[params.action.name]
    params << defaultParams << params

    jobDsl targets: [params.jobsPattern].join('\n'),
            removedJobAction: params.removedJobAction,
            removedViewAction: params.removedViewAction,
            lookupStrategy: params.lookupStrategy,
            additionalClasspath: params.additionalClasspath.join('\n')
}

