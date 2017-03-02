package com.github.aroq.drupipe.actions

def uploadArtifact(params) {
    def builder = params.builder
    def version = builder['version']
    def artifactId = builder['buildName']
    def fileName = builder['artifactFileName']
    def groupId = builder['groupId']

    if (artifactId && groupId && version && fileName) {
        // Upload artifact.
        nexusArtifactUploader(
            groupId: groupId,
            credentialsId: params.nexusCredentialsId,
            nexusUrl: params.nexusUrl,
            nexusVersion: params.nexusVersion,
            protocol: params.nexusProtocol,
            repository: params.nexusRepository,
            version: version,
            artifacts: [
                [
                    artifactId: artifactId,
                    classifier: '',
                    file: fileName,
                    type: params.nexusFileType
                ]
            ]
        )

        // Remove artifact.
        drupipeShell(
            "rm -f ${fileName}", params
        )
    }

    params
}
