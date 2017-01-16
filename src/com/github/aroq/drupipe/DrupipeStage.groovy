package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeAction> actions = []

    HashMap params = [:]

    def execute(params, body = null) {
        this.params = params
        this.params.pipeline.script.stage(name) {
            this.params.pipeline.script.gitlabCommitStatus(name) {
                if (body) {
                    this.params << body()
                }
                this.params << ['stage': this]
                if (actions) {
                    try {
                        for (action in this.actions) {
                            this.params << action.execute(s)
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

