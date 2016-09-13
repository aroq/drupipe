package com.github.aroq.workflowlibs.actions

def execute(params) {
    dir(params.docrootDir) {
        sh "${params.script}"
    }
}

