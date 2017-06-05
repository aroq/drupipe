package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class TaurusTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def test() {
//        def workspace = script.pwd()
        def sourcePath = utils.sourcePath(context, action.params.sourceName, '')
//        try {
            script.dir (sourcePath) {
                script.bzt """${context.jenkinsParams.taurus_config} \
-frontpage-cached -o \
execution.hold-for=${context.jenkinsParams.taurus_hold_for} \
-o execution.ramp-up=${context.jenkinsParams.taurus_ramp_up} -o \
execution.concurrency=${context.jenkinsParams.taurus_concurrency} \
${context.jenkinsParams.taurus_args}"""
            }
//            script.drupipeShell(
//
//"""cd ${sourcePath}; \
//${context.jenkinsParams.taurus_config} \
//-frontpage-cached -o \
//execution.hold-for=${context.jenkinsParams.taurus_hold_for} \
//-o execution.ramp-up=${context.jenkinsParams.taurus_ramp_up} -o \
//execution.concurrency=${context.jenkinsParams.taurus_concurrency} \
//${context.jenkinsParams.taurus_args}""",
//                context
//            )
//        }
//        catch (e) {
//            script.currentBuild.result = "UNSTABLE"
//            script.echo "Err: ${e}"
//        }
    }
}

