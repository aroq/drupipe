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

    def s3Upload() {
        def s3_credentials_id = (action.params.s3_credentials_id && action.params.s3_credentials_id.length() != 0) ? "${action.params.s3_credentials_id}" : false
        def s3_region = (action.params.s3_region && action.params.s3_region.length() != 0) ? "${action.params.s3_region}" : false
        def s3_bucket = (action.params.s3_bucket && action.params.s3_bucket.length() != 0) ? "${action.params.s3_bucket}" : false
        def s3_path = (action.params.s3_path && action.params.s3_path.length() != 0) ? "${action.params.s3_path}" : "index.html"
        def artifact_path = (action.params.artifact_path && action.params.artifact_path.length() != 0) ? "${action.params.artifact_path}" : "reports"

        try {
            if (s3_credentials_id == false || s3_region == false || s3_bucket == false) {
                throw new Exception("Check credentials, region and bucket settings.")
            }
            else {
                this.script.withAWS(credentials: s3_credentials_id, region: s3_region) {
                    this.script.s3Upload(bucket:s3_bucket, path: s3_path, file: artifact_path)
                }
            }
        }
        catch (e) {
            this.script.echo e.getMessage()
            this.script.archiveArtifacts artifacts: "${artifact_path}/**"
        }

        return [result: '']
    }
}

