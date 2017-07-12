package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class TaurusTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def test() {
        this.script.dir("logs") {
            this.script.deleteDir()
        }

        def hold_for = (this.context.taurus_hold_for.length() == 0) ? "-o execution.hold-for=${this.context.taurus_hold_for}" : ''
        def ramp_up = (this.context.taurus_ramp_up.length() == 0) ? "-o execution.rump-up=${this.context.taurus_ramp_up}" : ''
        def concurrency = (this.context.taurus_concurrency.length() == 0) ? "-o execution.concurrency=${this.context.taurus_concurrency}" : ''
        def throughput = (this.context.taurus_throughput.length() == 0) ? "-o execution.throughput=${this.context.taurus_throughput}" : ''
        def step = (this.context.taurus_steps.length() == 0) ? "-o execution.step=${this.context.taurus_steps}" : ''

        def bztString = """${this.context.taurus_config} \
${hold_for} \
${ramp_up} \
${concurrency} \
${throughput} \
${step} \
${this.context.taurus_args}"""

        this.script.echo "Execute BZT: ${bztString}"
        this.script.bzt "${bztString}"

        this.script.archiveArtifacts artifacts: 'logs/**'

    }
}
