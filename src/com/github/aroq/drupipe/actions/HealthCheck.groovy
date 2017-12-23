package com.github.aroq.drupipe.actions

class HealthCheck extends BaseAction {

    def wait_http_ok() {
        action.pipeline.drupipeLogger.warning "Kubectl_get_loadbalancer_address: ${action.pipeline.context.results.action.Kubectl_get_loadbalancer_address.url}"
        action.pipeline.drupipeLogger.warning "HealthCheck url: ${action.params.url}"
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}

