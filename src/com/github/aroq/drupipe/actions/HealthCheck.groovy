package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class HealthCheck extends BaseAction {

    def context

    def script

    def utils

    DrupipeAction action

    def wait_http_ok() {
        script.drupipeShell(
            "${action.params.full_command.join(' ')}",
            context,
            action.params
        )
    }

}

