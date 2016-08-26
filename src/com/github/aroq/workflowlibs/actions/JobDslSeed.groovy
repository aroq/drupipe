package com.github.aroq.actions.workflowlibs

def perform(params) {
    jobDsl targets: [params.jobsPattern].join('\n'),
            removedJobAction: 'DELETE',
            removedViewAction: 'DELETE',
            lookupStrategy: 'SEED_JOB',
            additionalClasspath: ['library/src'].join('\n')
}

