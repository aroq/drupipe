package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class HealthCheck extends BaseAction {

    def context

    def script

    def utils

    DrupipeActionWrapper action

    def wait_http_ok() {
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

