package com.github.aroq.workflowlibs.actions

def execute(params) {
    sh "${params.script} ${params.args.join(' ')}"
}

