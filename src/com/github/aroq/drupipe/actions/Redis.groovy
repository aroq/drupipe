package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class Redis extends BaseAction {

    def context

    def script

    def utils

    def DrupipeActionConroller action

    def runCommand() {
        def dsns = (this.context.redis_dsn.length() != 0) ? "${this.context.redis_dsn}" : ''
        def socket = (this.context.redis_socket.length() != 0) ? "-s ${this.context.redis_socket}" : ''
        def host = (this.context.redis_host.length() != 0) ? "-h ${this.context.redis_host}" : ''
        def port = (this.context.redis_port.length() != 0) ? "-s ${this.context.redis_port}" : ''
        def command = (this.context.redis_command.length() != 0) ? "${this.context.redis_command}" : 'INFO'

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
