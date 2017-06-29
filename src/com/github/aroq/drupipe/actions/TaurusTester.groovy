package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class TaurusTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def test() {
        def bztString = """${this.context.taurus_config} \
-o execution.hold-for=${this.context.taurus_hold_for} \
-o execution.ramp-up=${this.context.taurus_ramp_up} \
-o execution.concurrency=${this.context.taurus_concurrency} \
-o execution.throughput=${this.context.taurus_throughput} \
-o execution.step=${this.context.taurus_steps} \
${this.context.taurus_args}"""

        this.script.echo "Execute BZT: ${bztString}"
        this.script.bzt "${bztString}"

        this.script.archiveArtifacts artifacts: 'logs/*'

    }
}

