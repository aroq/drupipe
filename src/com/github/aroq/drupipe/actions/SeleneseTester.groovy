package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class SeleneseTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def test() {
        def workspace = script.pwd()
        def sourcePath = utils.sourcePath(context, action.params.sourceName, '')

        def suites = context.suites.split(",")
        for (def i = 0; i < suites.size(); i++) {
            script.drupipeShell("""docker pull ${action.params.dockerImage}""", context)
            try {
                script.drupipeShell("""docker run --rm --user root:root -v "${workspace}:${workspace}" \
-e "SELENESE_BASE_URL=${action.params.SELENESE_BASE_URL}" \
-e "SCREEN_WIDTH=1920" -e "SCREEN_HEIGHT=1080" -e "SCREEN_DEPTH=24" \
--workdir "${sourcePath}" \
--entrypoint "/opt/bin/entry_point.sh" --shm-size=2g ${action.params.dockerImage} "${suites[i]}"
    """, context)
            }
            catch (e) {
                script.currentBuild.result = "UNSTABLE"
            }
        }

        script.publishHTML (target: [
            allowMissing: false,
            alwaysLinkToLastBuild: false,
            keepAll: true,
            reportDir: "${sourcePath}/reports",
            reportFiles: 'index.html',
            reportName: "Selenese report"
        ])
    }
}

