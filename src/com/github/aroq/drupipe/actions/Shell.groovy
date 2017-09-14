package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Shell extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def execute() {
        def result = script.drupipeShell(action.params.shellCommand, this.context.clone() << [drupipeShellReturnStdout: true])
        this.context.lastActionOutput = result
    }
}
