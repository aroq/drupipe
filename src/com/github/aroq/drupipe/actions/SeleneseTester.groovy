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

        def suites = context.suites.split(",")
        for (def i = 0; i < suites.size(); i++) {
            script.drupipeShell("""docker pull ${action.params.dockerImage}""", context)
            try {
                script.drupipeShell("""docker run --rm --user root:root -v "${workspace}:${workspace}" \
-e "SELENESE_BASE_URL=${action.params.SELENESE_BASE_URL}" \
-e "SCREEN_WIDTH=1920" -e "SCREEN_HEIGHT=1080" -e "SCREEN_DEPTH=24" \
--workdir "${workspace}/${action.params.dir}/${action.params.repoDirName}" \
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
            reportDir: 'tests/selenese/reports',
            reportFiles: 'index.html',
            reportName: "Selenese report"
        ])

//        script.step([$class: 'SeleniumHtmlReportPublisher', testResultsDir: 'tests/selenese/reports-xml'])
//        script.junit 'reports-xml/*.xml'
    }
}

