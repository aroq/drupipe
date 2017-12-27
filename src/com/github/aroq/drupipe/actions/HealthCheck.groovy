package com.github.aroq.drupipe.actions

class HealthCheck extends BaseAction {

    def wait_http_ok() {
        action.pipeline.drupipeLogger.warning "address: ${action.pipeline.context.k8s.jenkins.address}"
        action.pipeline.drupipeLogger.warning "HealthCheck url: ${action.params.url}"
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

