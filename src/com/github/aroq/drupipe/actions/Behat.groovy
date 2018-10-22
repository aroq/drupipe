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
                cd ${action.params.masterPath}
                mkdir -p ${action.params.workspaceRelativePath}/reports
                ${action.params.masterRelativePath}/${action.params.behatExecutable} --config=${action.params.masterRelativePath}/${action.params.pathToEnvironmentConfig}/behat.${testEnvironment}.yml ${action.params.behat_args} --out=${action.params.workspaceRelativePath}/reports ${tags} ${features}
            """, action.params
                )
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

