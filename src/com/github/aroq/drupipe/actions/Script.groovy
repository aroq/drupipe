package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Script extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action
    def execute() {
        sh "${action.params.script} ${action.params.args.join(' ')}"
    }
}

