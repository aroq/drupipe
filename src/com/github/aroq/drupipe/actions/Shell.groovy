package com.github.aroq.drupipe.actions

class Shell extends BaseAction {
    def execute() {
        drupipeShell(action.params.shellCommand, context)
    }
}


