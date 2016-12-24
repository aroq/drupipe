package com.github.aroq.workflowlibs.actions

def perform(params) {
    echo 'JOBDSLSEED PERFORM'
    sh 'ls -l docroot/config/pipelines/jobdsl'
    jobDsl targets: params.jobsPattern.join('\n'),
            removedJobAction: params.removedJobAction,
            removedViewAction: params.removedViewAction,
            lookupStrategy: params.lookupStrategy,
            additionalClasspath: params.additionalClasspath.join('\n')
}

