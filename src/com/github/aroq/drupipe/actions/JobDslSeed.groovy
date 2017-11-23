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
                this.script.deleteDir()
            }
            script.dir('library') {
                this.script.deleteDir()
            }
            script.dir('mothership') {
                this.script.deleteDir()
            }
        }

        if (action.pipeline.context.tags && action.pipeline.context.tags.contains('docman')) {
            script.drupipeAction([action: 'Docman.info'], action.pipeline)
        }

        script.unstash 'config'
        if (script.fileExists("${action.pipeline.context.projectConfigPath}/pipelines/jobdsl")) {
            action.pipeline.context.params.action.JobDslSeed_perform.jobsPattern << "${action.pipeline.context.projectConfigPath}/pipelines/jobdsl/*.groovy"
        }
    }

    def perform() {
        action.pipeline.serializeObject('.unipipe/temp/context_processed.json', action.pipeline.context, 'json')
//        utils.dumpConfigFile(action.pipeline.context)
        action.pipeline.scripts_library_load()

        script.jobDsl targets: action.params.jobsPattern.join('\n'),
            removedJobAction: action.params.removedJobAction,
            removedViewAction: action.params.removedViewAction,
            lookupStrategy: action.params.lookupStrategy,
            additionalClasspath: action.params.additionalClasspath.join('\n')
    }
}
