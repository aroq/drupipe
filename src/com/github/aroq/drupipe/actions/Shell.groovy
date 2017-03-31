package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Shell extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action
    def execute() {
        drupipeShell(action.params.shellCommand, context)
    }
}


