package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeActionWrapper> actions = []

//    HashMap pipeline.context = [:]

    DrupipePipeline pipeline

    def execute(body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        utils.dump(pipeline.context, this.pipeline.context.params, 'DrupipeStage this.pipeline.context params BEFORE', true)
        this.pipeline.context.pipeline.script.stage(name) {
            this.pipeline.context.pipeline.script.gitlabCommitStatus(name) {
                if (body) {
                    // TODO: recheck it.
                    this.pipeline.context << body()
                }
                this.pipeline.context << ['stage': this]
                if (actions) {
                    try {
                        for (a in this.actions) {
                            if (pipeline.context && pipeline.context.action && pipeline.context.action["${a.name}_${a.methodName}"] && pipeline.context.action["${name}_${a.methodName}"].debugEnabled) {
                                utils.debugLog(pipeline.context, this.pipeline.context, "ACTION ${a.name}.${a.methodName} DrupipeStage.execute() BEFORE EXECUTE", [:], [], true)
                            }
                            utils.dump(pipeline.context, a, 'DrupipeStage a BEFORE EXECUTE', true)
                            a.pipeline.context = a.pipeline.context ? utils.merge(s.pipeline.context, pipeline.context) : pipeline.context
                            def action = new DrupipeActionWrapper(a)
                            def actionResult = action.execute().result
                            pipeline.context = pipeline.context ? utils.merge(pipeline.context, actionResult) : actionResult
                            if (pipeline.context.params) {
                                utils.dump(pipeline.context, pipeline.context.params, 'DrupipeStage this.pipeline.context AFTER', true)
                            }
                            if (pipeline.context && pipeline.context.action && pipeline.context.action["${a.name}_${a.methodName}"] && pipeline.context.action["${name}_${a.methodName}"].debugEnabled) {
                                utils.debugLog(pipeline.context, this.pipeline.context, "ACTION ${a.name}.${a.methodName} DrupipeStage.execute() AFTER EXECUTE", [:], [], true)
                            }
                        }
                        this.pipeline.context
                    }
                    catch (e) {
                        this.pipeline.context.pipeline.script.echo e.toString()
                        throw e
                    }
                }
            }
        }
        this.pipeline.context
    }

}
