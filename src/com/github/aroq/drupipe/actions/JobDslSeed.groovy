package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class JobDslSeed extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def perform() {
        utils.dumpConfigFile(action.pipeline.context)
        action.pipeline.scripts_library_load()

        script.jobDsl targets: action.params.jobsPattern.join('\n'),
            removedJobAction: action.params.removedJobAction,
            removedViewAction: action.params.removedViewAction,
            lookupStrategy: action.params.lookupStrategy,
            additionalClasspath: action.params.additionalClasspath.join('\n')
    }
}
