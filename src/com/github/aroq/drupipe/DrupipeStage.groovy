package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeAction> actions = []

    HashMap context = [:]

    def execute(body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        utils.dump(context, this.context.params, 'DrupipeStage this.context params BEFORE', true)
        this.context.pipeline.script.stage(name) {
            this.context.pipeline.script.gitlabCommitStatus(name) {
                if (body) {
                    this.context << body()
                }
                this.context << ['stage': this]
                if (actions) {
                    try {
                        for (a in this.actions) {
                            if (context && context.action && context.action["${a.name}_${a.methodName}"] && context.action["${name}_${a.methodName}"].debugEnabled) {
                                utils.debugLog(context, this.context, "ACTION ${a.name}.${a.methodName} DrupipeStage.execute() BEFORE EXECUTE", [:], [], true)
                            }
                            utils.dump(context, a, 'DrupipeStage a BEFORE EXECUTE', true)
                            a.context = a.context ? utils.merge(s.context, context) : context
                            def action = new DrupipeAction(a)
                            this.context << action.execute()
                            if (context.params) {
                                utils.dump(context, context.params, 'DrupipeStage this.context AFTER', true)
                            }
                            if (context && context.action && context.action["${a.name}_${a.methodName}"] && context.action["${name}_${a.methodName}"].debugEnabled) {
                                utils.debugLog(context, this.context, "ACTION ${a.name}.${a.methodName} DrupipeStage.execute() AFTER EXECUTE", [:], [], true)
                            }
                        }
                        this.context
                    }
                    catch (e) {
                        this.context.pipeline.script.echo e.toString()
                        throw e
                    }
                }
            }
        }
        this.context
    }

}
