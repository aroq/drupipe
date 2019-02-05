package com.github.aroq.drupipe.actions

class SeleneseTester extends BaseAction {

    def test() {
        def workspace = script.pwd()
        def sourcePath = utils.sourcePath(context, action.params.sourceName, '')

        def suites = context.suites.split(",")
        for (def i = 0; i < suites.size(); i++) {
            script.drupipeShell("""docker pull ${action.params.dockerImage}""", action.params)
            try {
                script.drupipeShell(
"""docker run --rm --user root:root -v "${workspace}:${workspace}" \
-e "SELENESE_BASE_URL=${action.params.SELENESE_BASE_URL}" \
-e "SCREEN_WIDTH=1920" -e "SCREEN_HEIGHT=1080" -e "SCREEN_DEPTH=24" \
--workdir "${workspace}/${sourcePath}" \
--entrypoint "/opt/bin/entry_point.sh" --shm-size=2g ${action.params.dockerImage} "${suites[i]}"
""", action.params)
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

    def test2() {
        def suites = action.pipeline.context.env.suites.split(",")
        for (def i = 0; i < suites.size(); i++) {
            try {
                def command = """
/opt/bin/entry_point.sh "${suites[i]}"
"""
                script.drupipeShell(command, action.params)
            }
            catch (e) {
                script.currentBuild.result = "UNSTABLE"
            }
        }

        try {
            this.script.publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'reports', reportFiles: 'index.html', reportName: 'Selenese', reportTitles: ''])
        }
        catch (e) {
            this.script.echo "Publish HTML plugin isn't installed. Use artifact instead."
            this.script.archiveArtifacts artifacts: 'reports/**'
        }

    }

}
