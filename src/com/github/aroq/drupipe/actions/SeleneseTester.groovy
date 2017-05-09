package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class SeleneseTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def test() {
        script.drupipeAction([action: "Git.clone", params: action.params], context)
        def workspace = script.pwd()

        def suites = context.suites.split("\n")
        for (def i = 0; i < suites.size(); i++) {
            script.drupipeShell("""docker run --rm --user root:root -v "${workspace}:${workspace}" \
-e "SELENESE_BASE_URL=${action.params.SELENESE_BASE_URL}" \
-e "SCREEN_WIDTH=1920" -e "SCREEN_HEIGHT=1080" -e "SCREEN_DEPTH=24" \
--workdir "${workspace}/${action.params.dir}/${action.params.repoDirName}" \
--entrypoint "/opt/bin/entry_point.sh" --shm-size=2g ${action.params.dockerImage} "${[suites[i]]}"
    """, context)
        }

        script.step([$class: 'SeleniumHtmlReportPublisher', testResultsDir: 'reports'])

    }

}

