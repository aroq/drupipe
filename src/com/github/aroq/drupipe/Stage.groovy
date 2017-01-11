package com.github.aroq.drupipe

class Stage implements Serializable {
    String name

    ArrayList<Action> actions = []

    HashMap params = [:]

    def script

    def execute(body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        script.stage(name) {
            script.gitlabCommitStatus(name) {
                if (body) {
                    params << body()
                }
                this.params << ['stage': this]
                if (actions) {
                    try {
                        for (action in this.actions) {
                            this.params << action.execute()
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

