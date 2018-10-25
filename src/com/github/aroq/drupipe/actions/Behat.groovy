package com.github.aroq.drupipe.actions

class Behat extends BaseAction {

    def perform() {
        def testEnvironment = action.pipeline.context.env.testEnvironment ? action.pipeline.context.env.testEnvironment : action.pipeline.context.environment
        def features = ''
        if (action.pipeline.context.env.features) {
            features = action.pipeline.context.env.features
        }
        def tags = ''
        if (action.pipeline.context.env.tags) {
            tags = "--tags=${action.pipeline.context.env.tags}"
        }

        // TODO: Add settings to exit with error on Behat errors.
        if (script.fileExists("${action.params.masterPath}/${action.params.behatExecutable}")) {
            if (script.fileExists("${action.params.masterPath}/${action.params.pathToEnvironmentConfig}/behat.${testEnvironment}.yml")) {
                script.drupipeShell(
                """
                cd ${action.pipeline.context.workspace}
                mkdir -p ${action.pipeline.context.workspace}/reports
                /opt/bin/entry_point.sh "${action.pipeline.context.workspace}/${action.params.behatExecutable} --config=${action.pipeline.context.workspace}/${action.params.pathToEnvironmentConfig}/behat.${testEnvironment}.yml ${action.params.behat_args} --out=${action.pipeline.context.workspace}/reports ${tags} ${features}"
                """, action.params
                )
                
                this.script.archiveArtifacts artifacts: 'reports/**'
                try {
                    this.script.publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'reports', reportFiles: 'index.html', reportName: 'Behat', reportTitles: ''])
                }
                catch (e) {
                    this.script.echo "Publish HTML plugin isn't installed."
                }
            }
            else {
                throw new Exception("Behat config file not found: ${action.params.masterPath}/${action.params.pathToEnvironmentConfig}/behat.${testEnvironment}.yml")
            }
        }
        else {
            throw new Exception("Behat execution file doesn't present: ${action.params.masterPath}/${action.params.behatExecutable}")
        }
    }

}

