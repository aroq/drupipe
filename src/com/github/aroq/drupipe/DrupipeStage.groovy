package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeAction> actions = []

    HashMap params = [:]

    def execute(params, body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        this.params = params
        this.params.pipeline.script.stage(name) {
            this.params.pipeline.script.gitlabCommitStatus(name) {
                if (body) {
                    this.params << body()
                }
                this.params << ['stage': this]
                if (actions) {
                    try {
                        for (a in this.actions) {
                            utils.jsonDump(a, 'ACTION BEFORE')
                            def action = new DrupipeAction(a)
                            utils.jsonDump(action, 'ACTION AFTER')
                            this.params << action.execute(this.params)
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

