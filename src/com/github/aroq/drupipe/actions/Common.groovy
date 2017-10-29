package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Common extends BaseAction {

    def context

    def script

    def utils

    def DrupipeActionWrapper action

    def confirm() {
        def message = context.message
        script.timeout(time: action.params.timeToConfirm, unit: 'MINUTES') {
           this.script.input 'Do you want to proceed?'
        }
    }
}
