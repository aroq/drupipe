package com.github.aroq.drupipe.actions

class BaseShellAction extends BaseAction {

    def execute() {
        if (!action.params.full_command) {
            action.pipeline.drupipeLogger.debugLog(action.pipeline.context, action.params, "ACTION PARAMS (full_command is absent)", [debugMode: 'json'])
        }
        script.drupipeShell("${action.params.full_command.join(' ')}", action.params)
    }

}