package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class Helm extends BaseAction {

    def context

    def script

    def utils

    DrupipeActionConroller action

    def init() {
        executeHelmCommand()
    }

    def apply() {
        executeHelmCommand()
    }

    def status() {
        executeHelmCommand()
    }

    def delete() {
        executeHelmCommand()
    }

    def executeHelmCommand() {
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

