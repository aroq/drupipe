package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class VegetaTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def prepare() {
        if (this.context.vegeta_prepare_command.length() != 0) {
            this.script.drupipeShell("mkdir -p vegeta", context)
            this.script.drupipeShell(this.context.vegeta_prepare_command, context)
        }
    }

    def test() {
        if (this.script.fileExists("vegeta/input.txt")) {
            this.script.drupipeShell("rm -rf vegeta/report.bin", context << [shellCommandWithBashLogin: true])

            def connections = (this.context.vegeta_connections.length() != 0) ? "-connections ${this.context.vegeta_connections}" : ''
            def duration = (this.context.vegeta_duration.length() != 0) ? "-duration ${this.context.vegeta_duration}" : ''
            def redirects = (this.context.vegeta_redirects.length() != 0) ? "-redirects ${this.context.vegeta_redirects}" : ''
            def rate = (this.context.vegeta_rate.length() != 0) ? "-rate ${this.context.vegeta_rate}" : ''
            def timeout = (this.context.vegeta_timeout.length() != 0) ? "-timeout ${this.context.vegeta_timeout}" : ''
            def workers = (this.context.vegeta_workers.length() != 0) ? "-workers ${this.context.vegeta_workers}" : ''
            def insecure = (this.context.vegeta_insecure.length() != 0) ? "-insecure" : ''
            def keepalive = (this.context.vegeta_keepalive.length() != 0) ? "-keepalive" : ''
            def lazy = (this.context.vegeta_lazy.length() != 0) ? "-lazy" : ''

            def vegetaAttackString = """vegeta attack \
-output vegeta/report.bin \
-targets vegeta/input.txt \
${connections} \
${duration} \
${rate} \
${redirects} \
${timeout} \
${workers} \
${insecure} \
${keepalive} \
${lazy} \
${this.context.vegeta_args}"""

            this.script.echo "Execute Vegeta attack: ${vegetaAttackString}"

            this.script.drupipeShell("""
                ls -lah
                ls -lah vegeta
                pwd
                """, context << [shellCommandWithBashLogin: true]
            )

            this.script.drupipeShell("${vegetaAttackString}", context)

            this.script.drupipeShell("vegeta report -inputs vegeta/report.bin", context)

            this.script.archiveArtifacts artifacts: 'vegeta/**'

        }
        else {
            throw new Exception("Vegeta input file not found: vegeta/input.txt")
        }
    }
}
