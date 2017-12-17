package com.github.aroq.drupipe.actions

class BaseShellAction extends BaseAction {

    def prepare() {
        if (!action.params.full_command) {
            action.pipeline.drupipeLogger.debugLog(action.pipeline.context, action.params, "ACTION PARAMS (full_command is absent)", [debugMode: 'json'])
        }
        action.params.full_command.join(' ')
    }

    def execute() {
        script.drupipeShell(prepare(), action.params)
    }

}