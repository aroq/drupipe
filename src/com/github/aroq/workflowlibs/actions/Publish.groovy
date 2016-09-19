package com.github.aroq.workflowlibs.actions

def junit(params) {
    step([$class: 'JUnitResultArchiver', testResults: params.reportsPath])
}


