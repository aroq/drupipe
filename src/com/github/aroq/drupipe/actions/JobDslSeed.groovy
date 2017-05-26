package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class JobDslSeed extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def perform() {
        utils.dumpConfigFile(context)
        utils.loadLibrary(script, context)
        utils.dump(action.params, 'ACTION PARAMS')

        script.jobDsl targets: action.params.jobsPattern.join('\n'),
            removedJobAction: action.params.removedJobAction,
            removedViewAction: action.params.removedViewAction,
            lookupStrategy: action.params.lookupStrategy,
            additionalClasspath: action.params.additionalClasspath.join('\n')
    }
}
