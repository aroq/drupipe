package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class BaseAction implements Serializable {

    public def script

    public com.github.aroq.drupipe.Utils utils

    public DrupipeActionWrapper action

    def execute () {
        action.pipeline.drupipeLogger.warning "Please override base execute action"
    }

    def methodMissing(String name, args) {
        action.pipeline.drupipeLogger.info "Method missing: ${name}"
        this.execute()
    }

}