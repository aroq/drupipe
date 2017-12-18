package com.github.aroq.drupipe.actions

class VegetaTester extends BaseAction {

    def prepare() {
        if (action.pipeline.context.env.vegeta_prepare_command && action.pipeline.context.env.vegeta_prepare_command.length() != 0) {
            this.script.drupipeShell("mkdir -p vegeta", action.params)
            this.script.drupipeShell(action.pipeline.context.env.vegeta_prepare_command, action.params)
            this.script.drupipeShell("""
                cat vegeta/input.txt
                """, action.params
            )
            this.script.stash name: 'vegeta', includes: "vegeta/**"
        }
    }

    def test() {
        this.script.unstash name: 'vegeta'
        if (this.script.fileExists("vegeta/input.txt")) {
            this.script.drupipeShell("rm -rf vegeta/report.bin", action.params)

            def connections = (action.pipeline.context.env.vegeta_connections && action.pipeline.context.env.vegeta_connections.length() != 0) ? "-connections ${action.pipeline.context.env.vegeta_connections}" : ''
            def duration = (action.pipeline.context.env.vegeta_duration && action.pipeline.context.env.vegeta_duration.length() != 0) ? "-duration ${action.pipeline.context.env.vegeta_duration}" : ''
            def redirects = (action.pipeline.context.env.vegeta_redirects && action.pipeline.context.env.vegeta_redirects.length() != 0) ? "-redirects ${action.pipeline.context.env.vegeta_redirects}" : ''
            def rate = (action.pipeline.context.env.vegeta_rate && action.pipeline.context.env.vegeta_rate.length() != 0) ? "-rate ${action.pipeline.context.env.vegeta_rate}" : ''
            def timeout = (action.pipeline.context.env.vegeta_timeout && action.pipeline.context.env.vegeta_timeout.length() != 0) ? "-timeout ${action.pipeline.context.env.vegeta_timeout}" : ''
            def workers = (action.pipeline.context.env.vegeta_workers && action.pipeline.context.env.vegeta_workers.length() != 0) ? "-workers ${action.pipeline.context.env.vegeta_workers}" : ''
            def insecure = (action.pipeline.context.env.vegeta_insecure && action.pipeline.context.env.vegeta_insecure.length() != 0) ? "-insecure" : ''
            def keepalive = (action.pipeline.context.env.vegeta_keepalive && action.pipeline.context.env.vegeta_keepalive.length() != 0) ? "-keepalive" : ''
            def lazy = (action.pipeline.context.env.vegeta_lazy && action.pipeline.context.env.vegeta_lazy.length() != 0) ? "-lazy" : ''

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
${action.pipeline.context.env.vegeta_args}"""

            this.script.echo "Execute Vegeta attack: ${vegetaAttackString}"

            this.script.drupipeShell("""
                ls -lah
                ls -lah vegeta
                pwd
                echo 'cat vegeta/input.txt'
                cat vegeta/input.txt
                """, action.params
            )

            this.script.drupipeShell("${vegetaAttackString}", action.params)

            this.script.drupipeShell("vegeta report -inputs vegeta/report.bin", action.params)

            this.script.archiveArtifacts artifacts: 'vegeta/**'

        }
        else {
            throw new Exception("Vegeta input file not found: vegeta/input.txt")
        }
    }
}
