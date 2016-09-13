package com.github.aroq.workflowlibs.actions

def execute(params) {
    sh "${params.script}"
}

