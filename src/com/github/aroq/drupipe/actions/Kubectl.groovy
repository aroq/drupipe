package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Kubectl extends BaseAction {

    def context

    def script

    def utils

    DrupipeAction action

    def scale() {
        executeKubectlCommand()
    }

    def getPods() {
        executeKubectlCommand()
    }

    def executeKubectlCommand() {
        script.drupipeShell("${action.params.full_command.join(' ')}", context << [shellCommandWithBashLogin: false])
    }

}

