package com.github.aroq.drupipe.actions

class TaurusTester extends BaseAction {

    def test() {
        this.script.dir("logs") {
            this.script.deleteDir()
        }

        this.script.checkout this.script.scm

        def hold_for = (action.pipeline.context.env.taurus_hold_for && action.pipeline.context.env.taurus_hold_for.length() != 0) ? "-o execution.hold-for=${action.pipeline.context.env.taurus_hold_for}" : ''
        def ramp_up = (action.pipeline.context.env.taurus_ramp_up && action.pipeline.context.env.taurus_ramp_up.length() != 0) ? "-o execution.ramp-up=${action.pipeline.context.env.taurus_ramp_up}" : ''
        def concurrency = (action.pipeline.context.env.taurus_concurrency && action.pipeline.context.env.taurus_concurrency.length() != 0) ? "-o execution.concurrency=${action.pipeline.context.env.taurus_concurrency}" : ''
        def throughput = (action.pipeline.context.env.taurus_throughput && action.pipeline.context.env.taurus_throughput.length() != 0) ? "-o execution.throughput=${action.pipeline.context.env.taurus_throughput}" : ''
        def steps = (action.pipeline.context.env.taurus_steps && action.pipeline.context.env.taurus_steps.length() != 0) ? "-o execution.steps=${action.pipeline.context.env.taurus_steps}" : ''
        def iterations = (action.pipeline.context.env.taurus_iterations && action.pipeline.context.env.taurus_iterations.length() != 0) ? "-o execution.iterations=${action.pipeline.context.env.taurus_iterations}" : ''

        def bztString = """${action.pipeline.context.env.taurus_config} \
${hold_for} \
${ramp_up} \
${concurrency} \
${throughput} \
${steps} \
${iterations} \
${action.pipeline.context.env.taurus_args}"""

        this.script.echo "Execute BZT: ${bztString}"

        this.script.bzt "${bztString}"

        this.script.archiveArtifacts artifacts: 'logs/**'

    }
}
