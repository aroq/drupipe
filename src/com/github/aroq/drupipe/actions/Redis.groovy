package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class VegetaTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def runCommand() {
        def socket = (this.context.redis_socket.length() != 0) ? "-s ${this.context.redis_socket}" : ''
        def host = (this.context.redis_host.length() != 0) ? "-h ${this.context.redis_host}" : ''
        def port = (this.context.redis_port.length() != 0) ? "-s ${this.context.redis_port}" : ''

        # Don't use host/port if socket available
        if (socket.length() != 0) {
            host = ''
            port = ''
        }

        def command = (this.context.redis_command.length() != 0) ? "${this.context.redis_command}" : 'INFO'

        def redisString = """redis-cli \
${socket} \
${host} \
${port} \
${command}"""

        this.script.echo "Execute Redis command: ${redisString}"

        this.script.drupipeShell("${rsedisString}", context)

    }
}
