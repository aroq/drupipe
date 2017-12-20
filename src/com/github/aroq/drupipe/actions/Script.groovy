package com.github.aroq.drupipe.actions

class Script extends BaseAction {

    def execute() {
        sh "${action.params.script} ${action.params.args.join(' ')}"
    }
}

