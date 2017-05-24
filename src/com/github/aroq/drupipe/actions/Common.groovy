package com.github.aroq.drupipe.actions

def confirm(params) {
    timeout(time: params.timeToConfirm, unit: 'MINUTES') {
        input params.message
    }
}