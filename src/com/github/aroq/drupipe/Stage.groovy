package com.github.aroq.drupipe

class Stage implements Serializable {
    String name

    ArrayList<Action> actions = []

    HashMap params = [:]

    def execute(body) {
        stage(name) {
            gitlabCommitStatus(name) {
                params << body()
                params << ['stage': stageInstance]
                if (actions) {
                    params << executeActions()
                }
            }
        }
        params
    }
}

def executeActions() {
    try {
        for (action in actions) {
            params << executeAction(action, params)
        }
        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }

    params
}
