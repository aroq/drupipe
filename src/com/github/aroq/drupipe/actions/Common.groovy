package com.github.aroq.drupipe.actions

class Common extends BaseAction {
    def confirm() {
        script.timeout(time: action.params.timeToConfirm, unit: 'MINUTES') {
            script.input context.message
        }
    }
}

