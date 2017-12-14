package com.github.aroq.drupipe.actions

class Nexus extends BaseAction {

    def uploadArtifact() {
        def builder = action.pipeline.builder
        def version = builder['version']
        def artifactId = builder['buildName']
        def fileName = builder['artifactFileName']
        def groupId = builder['groupId']

        if (artifactId && groupId && version && fileName) {
            // Upload artifact.
            script.nexusArtifactUploader(
                groupId: groupId,
                credentialsId: action.params.nexusCredentialsId,
                nexusUrl: action.params.nexusUrl,
                nexusVersion: action.params.nexusVersion,
                protocol: action.params.nexusProtocol,
                repository: action.params.nexusRepository,
                version: version,
                artifacts: [
                    [
                        artifactId: artifactId,
                        classifier: '',
                        file: fileName,
                        type: action.params.nexusFileType
                    ]
                ]
            )

            // Remove artifact.
            script.drupipeShell(
                "rm -f ${fileName}", action.params
            )
        }

        action.pipeline.context
    }
}


