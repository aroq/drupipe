package com.github.aroq.drupipe.actions

class Redis extends BaseAction {

    def runCommand() {
        def dsns = (action.pipeline.context.env.redis_dsn && action.pipeline.context.env.redis_dsn.length() != 0) ? "${action.pipeline.context.env.redis_dsn}" : ''
        def socket = (action.pipeline.context.env.redis_socket && action.pipeline.context.env.redis_socket.length() != 0) ? "-s ${action.pipeline.context.env.redis_socket}" : ''
        def host = (action.pipeline.context.env.redis_host && action.pipeline.context.env.redis_host.length() != 0) ? "-h ${action.pipeline.context.env.redis_host}" : ''
        def port = (action.pipeline.context.env.redis_port && action.pipeline.context.env.redis_port.length() != 0) ? "-s ${action.pipeline.context.env.redis_port}" : ''
        def command = (action.pipeline.context.env.redis_command && action.pipeline.context.env.redis_command.length() != 0) ? "${action.pipeline.context.env.redis_command}" : 'INFO'

        // Do not use socket/host/port if dsn available
        if (dsns != '') {
            def dsn_array = dsns.split("\\|")
            for (def i = 0; i < dsn_array.length; i++) {
                def dsn = dsn_array[i]
                if (dsn ==~ /^.*:[0-9]+$/) {
                    def host_port = dsn.split(":")
                    def redisString = """redis-cli \
-h ${host_port[0]} \
-p ${host_port[1]} \
${command}"""
                    this.script.echo "Execute Redis command: ${redisString}"
                    this.script.drupipeShell("${redisString}", action.params)
                }
                else {
                    def redisString = """redis-cli \
-s ${dsn} \
${command}"""
                    this.script.echo "Execute Redis command: ${redisString}"
                    this.script.drupipeShell("${redisString}", action.params)
                }
            }
        }
        // Do not use host/port if socket available
        else if (socket != '') {
            host = ''
            port = ''

            def redisString = """redis-cli \
${socket} \
${command}"""
            this.script.echo "Execute Redis command: ${redisString}"
            this.script.drupipeShell("${redisString}", action.params)
        }
        else {
            def redisString = """redis-cli \
${host} \
${port} \
${command}"""
            this.script.echo "Execute Redis command: ${redisString}"
            this.script.drupipeShell("${redisString}", action.params)
        }

    }
}
