package com.github.aroq.drupipe.actions

class Commands extends BaseAction {

    def execute() {
        def commands = []
        if (!action.params.commands) {
            action.pipeline.drupipeLogger.error "Commands are not defined"
        }

        if (action.params.aggregate_commands) {
            commands.add(action.params.execution_dir + ' && ' + action.params.commands.join(' && '))
            action.pipeline.drupipeLogger.error "Commands are not defined"
        }
        else {
            commands = action.params.commands.collect(action.params.execution_dir + ' && ' + it)
        }

        for (command in commands) {
            action.pipeline.drupipeLogger.info "Execute command: ${command}"
        }
    }
}
