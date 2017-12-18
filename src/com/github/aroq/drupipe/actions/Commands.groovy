package com.github.aroq.drupipe.actions

class Commands extends BaseAction {

    def execute() {
        action.pipeline.drupipeLogger.debugLog(action.pipeline.context, action.params, "ACTION.PARAMS", [debugMode: 'json'])
        def commands = []
        if (action.params.actions.size() == 0) {
            action.pipeline.drupipeLogger.error "Commands are not defined"
        }

//        for (command in action.params.actions) {
//            if (command instanceof HashMap) {
//                def a = action.pipeline.drupipeConfig.processItem(command, 'actions', 'params', 'execute')
//                command =
//            }
//        }

        if (action.params.aggregate_commands) {
            commands.add("cd ${action.params.execution_dir}" + ' && ' + action.params.actions.join(' && '))
        }
        else {
            commands = action.params.actions.collect("cd {$action.params.execution_dir}" + ' && ' + it)
        }

        def prepareSSHChainCommand = { String command, int level ->
            command = command.replaceAll("\\\\", "\\\\\\\\")
            return command.replaceAll("\"", "\\\\\"")
        }

        String chainCommand
        for (command in commands) {
            action.pipeline.drupipeLogger.trace "Execute command: ${command}"
            chainCommand = command
            if (action.params.containsKey('through_ssh_chain')) {
                int level = action.params.through_ssh_chain.size()
                for (String sshChainItem in action.params.through_ssh_chain.reverse()) {
                    chainCommand = /${action.params.through_ssh_params.executable} ${action.params.through_ssh_params.options} ${sshChainItem} "${prepareSSHChainCommand(chainCommand, level)}"/
                    action.pipeline.drupipeLogger.trace "Level: ${level}, Command: ${chainCommand}"
                    level--
                }

                action.pipeline.drupipeLogger.trace "Execute command: ${chainCommand}"
                script.drupipeShell(chainCommand, action.params)
            }
        }
    }
}
