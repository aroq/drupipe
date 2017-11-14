package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class JobDslSeed extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def prepare() {
        script.checkout script.scm
        if (script.fileExists(action.pipeline.context.projectConfigPath)) {
            script.dir(action.pipeline.context.projectConfigPath) {
                script.deleteDir()
            }
            script.dir('library') {
                script.deleteDir()
            }
            script.dir('mothership') {
                script.deleteDir()
            }
        }

        script.unstash 'config'
        if (script.fileExists("${action.pipeline.context.projectConfigPath}/pipelines/jobdsl")) {
            action.pipeline.context.params.action.JobDslSeed_perform.jobsPattern << "${action.pipeline.context.projectConfigPath}/pipelines/jobdsl/*.groovy"
        }
    }

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
