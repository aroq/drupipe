package com.github.aroq.drupipe

class Stage implements Serializable {
    String name

    ArrayList<Action> actions = []

    HashMap params = [:]

    def script

    def execute(body = null) {
        script.stage(name) {
            script.gitlabCommitStatus(name) {
                if (body) {
                    params << body()
                }
                params << ['stage': this]
                if (actions) {
                    params << executeActions()
                }
            }
        }
        params
    }

    def executeActions() {
        def utils = new com.github.aroq.drupipe.Utils()
        try {
            for (action in this.actions) {
                this.params << utils.executeAction(action, this.params)
            }
            this.params
        }
        catch (e) {
            script.echo e.toString()
            throw e
        }

        this.params
    }
}

