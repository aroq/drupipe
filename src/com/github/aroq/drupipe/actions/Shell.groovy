package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Shell extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def execute() {
        if (context.shellOutputReturn) {
            this.script.echo "Return Shell output."
            def result = script.drupipeShell(action.params.shellCommand, this.context.clone() << [drupipeShellReturnStdout: true])
            this.context.lastActionOutput = result.drupipeShellResult
            return result
        }
        else {
            script.drupipeShell(action.params.shellCommand, this.context.clone())
        }
    }
}
