package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeAction> actions = []

    HashMap params = [:]

    def execute(params, body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        this.params = params
        utils.dump(params, this.params, 'DrupipeStage this.params BEFORE', true)
        this.params.pipeline.script.stage(name) {
            this.params.pipeline.script.gitlabCommitStatus(name) {
                if (body) {
                    this.params << body()
                }
                this.params << ['stage': this]
                if (actions) {
                    try {
                        for (a in this.actions) {
                            if (params && params.action && params.action["${a.name}_${a.methodName}"] && params.action["${name}_${a.methodName}"].debugEnabled) {
                                utils.debugLog(params, this.params, "ACTION ${a.name}.${a.methodName} DrupipeStage.execute() BEFORE EXECUTE", [:], [], true)
                            }
                            utils.dump(params, this.params, 'DrupipeStage a BEFORE EXECUTE', true)
                            def action = new DrupipeAction(a)
                            this.params << action.execute(this.params)
                            utils.dump(params, this.params, 'DrupipeStage this.params AFTER', true)
                            if (params && params.action && params.action["${a.name}_${a.methodName}"] && params.action["${name}_${a.methodName}"].debugEnabled) {
                                utils.debugLog(params, this.params, "ACTION ${a.name}.${a.methodName} DrupipeStage.execute() AFTER EXECUTE", [:], [], true)
                            }
                        }
                        this.params
                    }
                    catch (e) {
                        this.params.pipeline.script.echo e.toString()
                        throw e
                    }
                }
            }
        }
        this.params
    }

}
