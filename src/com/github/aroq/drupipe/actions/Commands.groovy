package com.github.aroq.drupipe.actions

class Commands extends BaseAction {

    def execute() {
        action.pipeline.drupipeLogger.debugLog(action.pipeline.context, action.params, "ACTION.PARAMS", [debugMode: 'json'])
        def commands = []
        if (action.params.commands.size() == 0) {
            action.pipeline.drupipeLogger.error "Commands are not defined"
        }

        if (action.params.aggregate_commands) {
            commands.add("cd ${action.params.execution_dir}" + ' && ' + action.params.commands.join(' && '))
            action.pipeline.drupipeLogger.error "Commands are not defined"
        }
        else {
            commands = action.params.commands.collect("cd {$action.params.execution_dir}" + ' && ' + it)
        }

        def prepareSSHChainCommand = { String command, int level ->
            command = command.replaceAll("\\\\", "\\\\\\\\")
            return command.replaceAll("\"", "\\\\\"")
        }

        for (command in commands) {
            action.pipeline.drupipeLogger.info "Execute command: ${command}"
            if (action.params.containsKey('through_ssh_chain')) {
                int level = action.params.through_ssh_chain.size()
                String chainCommand = command
                for (String sshChainItem in action.params.through_ssh_chain.reverse()) {
                    chainCommand = /${action.params.through_ssh_params.executable} ${action.params.through_ssh_params.options} ${sshChainItem} "${prepareSSHChainCommand(chainCommand, level)}"/
                    action.pipeline.drupipeLogger.log "Level: ${level}, Command: ${chainCommand}"
                    level--
                }

                action.pipeline.drupipeLogger.log "Command: ${chainCommand}"
            }
        }
    }
}
