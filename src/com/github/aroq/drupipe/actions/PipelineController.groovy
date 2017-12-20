package com.github.aroq.drupipe.actions

class PipelineController extends BaseAction {

    def build() {
        if (!action.pipeline.context['builder']) {
            action.pipeline.context['builder'] = [:]
        }
        if (action.params.buildHandler && action.params.buildHandler.handler) {
            // Dispatch the action.
            script.drupipeAction([action: "${action.params.buildHandler.handler}.${action.params.buildHandler.method}"], action.pipeline)
        }
        else {
            script.echo "No builder handler defined"
        }
        [:]
    }

    // TODO: move to artifact handler.
    def createArtifact() {
        def sourceDir = action.pipeline.context.builder['buildDir']
        def fileName = "${action.pipeline.context.builder['buildName']}-${action.pipeline.context.builder['version']}.tar.gz"
        action.pipeline.context.builder['artifactFileName'] = fileName
        action.pipeline.context.builder['groupId'] = action.pipeline.context.jenkinsFolderName

        script.drupipeShell(
            """
                rm -fR ${sourceDir}/.git
                tar -czf ${fileName} ${sourceDir}
            """, action.params
        )
        [:]
    }

    def deploy() {
        if (action.params.deployHandler && action.params.deployHandler.handler) {
            retrieveArtifact()
            script.drupipeAction([action: "${action.params.deployHandler.handler}.${action.params.deployHandler.method}", params: action.pipeline.context.builder.artifactParams], action.pipeline)
        }
        else {
            script.echo "No deploy handler defined"
        }
        [:]
    }

    def operations() {
        if (action.pipeline.context.env.operationsMode == 'no-ops') {
            script.echo "No operations mode (no-ops) is selected"
        }
        else {
            if (action.params.operationsHandler && action.params.operationsHandler.handler) {
                retrieveArtifact()
                script.drupipeAction([action: "${action.params.operationsHandler.handler}.${action.params.operationsHandler.method}"], action.pipeline)
            }
            else {
                script.echo "No operations handler defined"
            }
        }
        [:]
    }

    def test() {
        if (action.params.testHandler && action.params.testHandler.handler) {
            script.drupipeAction([action: "${action.params.testHandler.handler}.${action.params.testHandler.method}"], action.pipeline)
        }
        else {
            script.echo "No test handler defined"
        }
        [:]
    }

    def retrieveArtifact() {
        if (!action.pipeline.context['builder']) {
            action.pipeline.context['builder'] = [:]
        }
        if (action.params.artifactHandler && action.params.artifactHandler.handler) {
            //script.drupipeAction([action: "${action.params.buildHandler.handler}.artifactParams"], action.pipeline.context)
            artifactParams()
            script.drupipeAction([action: "${action.params.artifactHandler.handler}.${action.params.artifactHandler.method}", params: action.pipeline.context.builder.artifactParams], action.pipeline)
            if (!action.pipeline.context.projectName) {
                action.pipeline.context.projectName = 'master'
            }
        }
        else {
            script.echo "No artifact handler defined"
        }
        [:]
    }

    def repoParams(String configPath) {
        def repo
        def masterInfoFile = "${action.pipeline.context.projectConfigPath}/${configPath}/info.yaml"
        if (script.fileExists(masterInfoFile)) {
            def masterConfig = script.readYaml(file: masterInfoFile)
            script.echo "MASTER CONFIG: ${masterConfig}"
            repo = masterConfig.type == 'root' ? masterConfig.repo : masterConfig.root_repo
        }
        else {
            repo = action.pipeline.context.components.master.root_repo ? action.pipeline.context.components.master.root_repo : action.pipeline.context.components.master.repo
        }
        script.echo "REPO: ${repo}"

        String reference
        if (action.pipeline.context.jenkinsParams.release) {
            reference = action.pipeline.context.jenkinsParams.release
        }
        else {
            reference = action.pipeline.context.environmentParams.git_reference
        }
        script.echo "reference: ${reference}"
        return [
            repoAddress: repo,
            reference: reference,
            // TODO: refactor it.
            projectName: configPath,
        ]
    }

    def artifactParams() {
        action.pipeline.context.builder.artifactParams = repoParams('master')
        [:]
    }

}
