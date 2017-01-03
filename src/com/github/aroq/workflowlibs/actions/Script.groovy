package com.github.aroq.drupipe.actions

def execute(params) {
    sh "${params.script} ${params.args.join(' ')}"
}

