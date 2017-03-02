package com.github.aroq.drupipe.actions

def build(params) {
    if (!params['builder']) {
        params['builder'] = [:]
    }
    // Dispatch the action.
    params << drupipeAction([action: "${params.builderHandler}.${params.builderMethod}"], params)
    params << [returnConfig: true]
}

def createArtifact(params) {
    def sourceDir = params.builder['buildDir']
    def fileName = "${params.builder['buildName']}-${params.builder['version']}.tar.gz"
    params.builder['artifactFileName'] = fileName
    params.builder['groupId'] = params.jenkinsFolderName

    drupipeShell(
        """
        rm -fR ${sourceDir}/.git
        tar -czf ${fileName} ${sourceDir}
    """, params << [shellCommandWithBashLogin: true]
    )
    params << [returnConfig: true]
}

