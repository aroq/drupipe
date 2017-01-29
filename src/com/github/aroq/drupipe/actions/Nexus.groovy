package com.github.aroq.drupipe.actions

@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

import com.github.aroq.StateStableInfo

def deploy(params) {
    if (params.projectName) {
        echo "PROJECT NAME: ${params.projectName}"
        def fileBaseName = "${params.projectName}"

        dir("${params.projectName}") {
            git credentialsId: params.credentialsID, url: env.gitlabSourceRepoURL, branch: env.gitlabSourceBranch
        }

        StateStableInfo stateStableInfo = getStableTag(readFile("${fileBaseName}/info.yaml"))
        echo "VERSION: ${stateStableInfo.version}"

        if (params.nexusReleaseType == 'release') {
            params.nexusReleaseVersion = stateStableInfo.version
        }
        else {
            params.nexusReleaseVersion = 'SNAPSHOT'
        }

        fileName = "${fileBaseName}-${params.nexusReleaseVersion}.tar.gz"
        
        sh """#!/bin/bash -l
            rm -fR ${params.projectName}
            git clone --depth 1 -b ${stateStableInfo.version} ${env.gitlabSourceRepoURL} ${fileBaseName}
            rm -fR ${fileBaseName}/.git
            tar -czvf ${fileName} ${fileBaseName}
            ls -l
        """

        echo "fileBaseName: ${fileBaseName}"

        nexusArtifactUploader(
            groupId: env.gitlabSourceNamespace.toLowerCase(),
            credentialsId: params.nexusCredentialsId,
            nexusUrl: params.nexusUrl,
            nexusVersion: params.nexusVersion,
            protocol: params.nexusProtocol,
            repository: params.nexusRepository,
            version: params.nexusReleaseVersion,
            artifacts: [
                [
                    artifactId: params.projectName,
                    classifier: '',
                    file: fileName,
                    type: params.nexusFileType
                ]
          ]
       )
       sh "rm -f ${fileName}"
    }
    params
}

@NonCPS
def getStableTag(yamlFile) {
    Yaml yaml = new Yaml();
    StateStableInfo stableStableInfo = yaml.loadAs(yamlFile, StateStableInfo.class);
    return stableStableInfo
}
