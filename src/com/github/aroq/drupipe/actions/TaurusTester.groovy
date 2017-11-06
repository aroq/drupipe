package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class TaurusTester extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def test() {
        this.script.dir("logs") {
            this.script.deleteDir()
        }

        this.script.checkout this.script.scm

        def hold_for = (action.pipeline.context.taurus_hold_for && action.pipeline.context.taurus_hold_for.length() != 0) ? "-o execution.hold-for=${action.pipeline.context.taurus_hold_for}" : ''
        def ramp_up = (action.pipeline.context.taurus_ramp_up && action.pipeline.context.taurus_ramp_up.length() != 0) ? "-o execution.ramp-up=${action.pipeline.context.taurus_ramp_up}" : ''
        def concurrency = (action.pipeline.context.taurus_concurrency && action.pipeline.context.taurus_concurrency.length() != 0) ? "-o execution.concurrency=${action.pipeline.context.taurus_concurrency}" : ''
        def throughput = (action.pipeline.context.taurus_throughput && action.pipeline.context.taurus_throughput.length() != 0) ? "-o execution.throughput=${action.pipeline.context.taurus_throughput}" : ''
        def steps = (action.pipeline.context.taurus_steps && action.pipeline.context.taurus_steps.length() != 0) ? "-o execution.steps=${action.pipeline.context.taurus_steps}" : ''
        def iterations = (action.pipeline.context.taurus_iterations && action.pipeline.context.taurus_iterations.length() != 0) ? "-o execution.iterations=${action.pipeline.context.taurus_iterations}" : ''

        def bztString = """${action.pipeline.context.taurus_config} \
${hold_for} \
${ramp_up} \
${concurrency} \
${throughput} \
${steps} \
${iterations} \
${action.pipeline.context.taurus_args}"""

        this.script.echo "Execute BZT: ${bztString}"

        this.script.bzt "${bztString}"

        this.script.archiveArtifacts artifacts: 'logs/**'

    }
}
