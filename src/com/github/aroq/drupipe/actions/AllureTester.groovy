package com.github.aroq.drupipe.actions

class AllureTester extends BaseAction {

    def test() {

        def execute = (action.pipeline.context.env.allure_execute && action.pipeline.context.env.allure_execute.length() != 0) ? action.pipeline.context.env.allure_execute : ''
        def report_dir = (action.pipeline.context.env.allure_report_dir && action.pipeline.context.env.allure_report_dir.length() != 0) ? action.pipeline.context.env.allure_report_dir : ''

        script.dir(report_dir) {
            this.script.deleteDir()
        }

        script.drupipeShell(
"""
/opt/bin/entry_point.sh "${action.pipeline.context.workspace}/${execute}"
""", action.params)

        script.allure includeProperties: false, jdk: '', properties: [], reportBuildPolicy: 'ALWAYS', report: "${report_dir}/generated-report", results: [[path: report_dir]]

        script.archiveArtifacts artifacts: "${report_dir}/**"
    }
}
