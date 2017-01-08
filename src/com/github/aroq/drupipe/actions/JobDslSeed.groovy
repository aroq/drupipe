package com.github.aroq.drupipe.actions

def perform(params) {
    utils = new com.github.aroq.drupipe.Utils()
    utils.dumpConfigFile(params)
    utils.loadLibrary(this, params)

    jobDsl targets: params.jobsPattern.join('\n'),
            removedJobAction: params.removedJobAction,
            removedViewAction: params.removedViewAction,
            lookupStrategy: params.lookupStrategy,
            additionalClasspath: params.additionalClasspath.join('\n')
}

