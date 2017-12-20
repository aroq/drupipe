package com.github.aroq.drupipe.actions

class Shell extends BaseAction {

    def execute() {
        if (action.params.store_result) {
            this.script.echo "Return Shell output."
            def result = script.drupipeShell(action.params.shellCommand, [return_stdout: true])
            result.result = action.params.shellCommand
            return result
        }
        else {
            script.drupipeShell(action.params.shellCommand, action.params)
        }
    }
}
