package com.github.aroq.drupipe

class Stage implements Serializable {
    String name

    ArrayList<Action> actions = []

    HashMap params = [:]

    def script

    def execute(body = null) {
        this.script.stage(name) {
            script.gitlabCommitStatus(name) {
                if (body) {
                    params << body()
                }
                this.params << ['stage': this]
                if (actions) {
                    try {
                        for (action in this.actions) {
                            script.echo "EXECUTE ACTION PARAMS: ${params}"
                            this.params << action.execute(params)
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

