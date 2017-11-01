package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class TaurusTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeActionWrapper action

    def test() {
        this.script.dir("logs") {
            this.script.deleteDir()
        }

        this.script.checkout this.script.scm

        def hold_for = (this.context.taurus_hold_for && this.context.taurus_hold_for.length() != 0) ? "-o execution.hold-for=${this.context.taurus_hold_for}" : ''
        def ramp_up = (this.context.taurus_ramp_up && this.context.taurus_ramp_up.length() != 0) ? "-o execution.ramp-up=${this.context.taurus_ramp_up}" : ''
        def concurrency = (this.context.taurus_concurrency && this.context.taurus_concurrency.length() != 0) ? "-o execution.concurrency=${this.context.taurus_concurrency}" : ''
        def throughput = (this.context.taurus_throughput && this.context.taurus_throughput.length() != 0) ? "-o execution.throughput=${this.context.taurus_throughput}" : ''
        def steps = (this.context.taurus_steps && this.context.taurus_steps.length() != 0) ? "-o execution.steps=${this.context.taurus_steps}" : ''
        def iterations = (this.context.taurus_iterations && this.context.taurus_iterations.length() != 0) ? "-o execution.iterations=${this.context.taurus_iterations}" : ''

        def bztString = """${this.context.taurus_config} \
${hold_for} \
${ramp_up} \
${concurrency} \
${throughput} \
${steps} \
${iterations} \
${this.context.taurus_args}"""

        this.script.echo "Execute BZT: ${bztString}"

        this.script.bzt "${bztString}"

        this.script.archiveArtifacts artifacts: 'logs/**'

    }
}
