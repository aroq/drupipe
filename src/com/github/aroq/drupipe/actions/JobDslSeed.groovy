package com.github.aroq.drupipe.actions

class JobDslSeed extends BaseAction {
    def perform() {
        utils.dumpConfigFile(context)
        utils.loadLibrary(script, context)

        script.jobDsl targets: action.params.jobsPattern.join('\n'),
            removedJobAction: action.params.removedJobAction,
            removedViewAction: action.params.removedViewAction,
            lookupStrategy: action.params.lookupStrategy,
            additionalClasspath: action.params.additionalClasspath.join('\n')
    }
}
