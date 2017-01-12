package com.github.aroq.drupipe

class Stage implements Serializable {
    String name

    ArrayList<Action> actions = []

    HashMap params = [:]

    def script

    def execute(params, body = null) {
        this.params = params
        this.script.stage(name) {
            script.gitlabCommitStatus(name) {
                if (body) {
                    this.params << body()
                }
                this.params << ['stage': this]
                if (actions) {
                    try {
                        for (action in this.actions) {
                            script.echo "EXECUTE ACTION PARAMS: ${this.params}"
                            this.params << action.execute(this.params)
                        }
                        this.params
                    }
                    catch (e) {
                        script.echo e.toString()
                        throw e
                    }
                }
            }
        }
        this.params
    }

}

