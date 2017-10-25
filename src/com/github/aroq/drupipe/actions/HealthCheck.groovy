package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class HealthCheck extends BaseAction {

    def context

    def script

    def utils

    DrupipeAction action

    def wait_http_ok() {
//        def url = script.drupipeAction([action: "Kubectl.get_lb_address", params: action.params], context).drupipeShellResult
        script.drupipeShell(
            "${action.params.full_command.join(' ')}",
            context,
            action.params
        )
    }

}

