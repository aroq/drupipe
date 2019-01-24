package com.github.aroq.drupipe.actions

class Artifact extends BaseAction {

    def publishHtml() {
        def artifact_path = (action.params.artifact_path && action.params.artifact_path.length() != 0) ? "${action.params.artifact_path}" : "reports"
        def artifact_name = (action.params.artifact_name && action.params.artifact_name.length() != 0) ? "${action.params.artifact_name}" : "HTML"
        def artifact_files = (action.params.artifact_files && action.params.artifact_files.length() != 0) ? "${action.params.artifact_files}" : "index.html"
        try {
            this.script.publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: artifact_path, reportFiles: artifact_files, reportName: artifact_name, reportTitles: ''])
        }
        catch (e) {
            this.script.echo "Publish HTML plugin isn't installed. Use artifact instead."
            this.script.archiveArtifacts artifacts: "${artifact_path}/**"
        }
    }

    def artifact() {
        def artifact_path = (action.params.artifact_path && action.params.artifact_path.length() != 0) ? "${action.params.artifact_path}" : "reports"
        this.script.archiveArtifacts artifacts: "${artifact_path}/**"
    }

}

