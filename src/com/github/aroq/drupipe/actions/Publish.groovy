package com.github.aroq.drupipe.actions

def junit(params) {
    step([$class: 'JUnitResultArchiver', testResults: params.reportsPath])
}


