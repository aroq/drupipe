package com.github.aroq.drupipe.actions

class HealthCheck extends BaseAction {

    def wait_http_ok() {
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

