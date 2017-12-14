package com.github.aroq.drupipe.actions

class Publish extends BaseAction {

    def junit() {
        script.step([$class: 'JUnitResultArchiver', testResults: action.params.reportsPath])
    }
}



