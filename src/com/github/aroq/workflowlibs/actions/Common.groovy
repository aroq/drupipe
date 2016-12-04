package com.github.aroq.workflowlibs.actions

def confirm(params) {
    timeout(time: 60, unit: 'MINUTES') {
        input params.message
    }
}
