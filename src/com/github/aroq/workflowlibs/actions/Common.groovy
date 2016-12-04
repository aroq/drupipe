package com.github.aroq.workflowlibs.actions

def confirm(params) {
    timeout(time: params.timeToConfirm, unit: 'MINUTES') {
        input params.message
    }
}
