package com.github.aroq.drupipe.actions

class JobDslSeed extends BaseAction {

    def info() {
        if (action.pipeline.configVersion() > 1 && action.pipeline.context.tags && action.pipeline.context.tags.contains('docman')) {
            action.pipeline.executeAction(action: 'Docman.info')
            def stashes = action.pipeline.context.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')
            stashes = stashes + ", ${action.pipeline.context.docmanDir}/config/config.json"
            script.stash name: 'config', includes: "${stashes}", excludes: '.git, .git/**'
        }
    }

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

        script.unstash 'config'
        if (script.fileExists("${action.pipeline.context.projectConfigPath}/pipelines/jobdsl")) {
            action.pipeline.context.params.action.JobDslSeed_perform.jobsPattern << "${action.pipeline.context.projectConfigPath}/pipelines/jobdsl/*.groovy"
        }
    }

    def perform() {
        // Serialize processed context to pass it into Job DSL.
        action.pipeline.serializeObject('.unipipe/temp/context_processed.json', action.pipeline.context, 'json')

        // Load library (drupipe) to have DSL scripts available.
        action.pipeline.scripts_library_load()

        script.jobDsl targets: action.params.dsl_params.jobsPattern.join('\n'),
            removedJobAction: action.params.dsl_params.removedJobAction,
            removedViewAction: action.params.dsl_params.removedViewAction,
            lookupStrategy: action.params.dsl_params.lookupStrategy,
            additionalClasspath: action.params.dsl_params.additionalClasspath.join('\n')
    }
}
