package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Publish extends BaseAction {

    def context

    def script

    def utils

    def DrupipeActionWrapper action
    def junit() {
        script.step([$class: 'JUnitResultArchiver', testResults: action.params.reportsPath])
    }
}



