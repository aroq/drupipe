package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class HealthCheck extends BaseAction {

    def context

    def script

    def utils

    DrupipeActionConroller action

    def wait_http_ok() {
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

