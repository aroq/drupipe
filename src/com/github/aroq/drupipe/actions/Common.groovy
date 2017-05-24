package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Common extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def confirm() {
        def message = context.message
        script.timeout(time: action.params.timeToConfirm, unit: 'MINUTES') {
            input 'Do you want to proceed?'
        }
    }
}

