package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class Shell extends BaseAction {

    def context

    def script

    def utils

    def DrupipeActionConroller action

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
