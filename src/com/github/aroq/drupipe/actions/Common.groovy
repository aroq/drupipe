package com.github.aroq.drupipe.actions

class Common extends BaseAction {

    def confirm() {
        def message = action.pipeline.context.message
        script.timeout(time: action.params.timeToConfirm, unit: 'MINUTES') {
           this.script.input 'Do you want to proceed?'
        }
    }
}
