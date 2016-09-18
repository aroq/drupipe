package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    if (params.deployFlowEnvironment) {
        deployEnvironment = params.deployFlowEnvironment
    }
    else {
        deployEnvironment = environment
    }

    echo("deployEnvironment: ${deployEnvironment}")
    echo("params.deployFlowConfirm?.environment: ${params.deployFlowConfirm?.environment}")

    if (deployEnvironment == params.deployFlowConfirm?.environment) {
        timeout(time: 10, unit: 'MINUTES') {
            input params.deployFlowConfirm?.message
        }
    }

    options = ''
    if (fileExists(file: params.propertiesFile)) {
        options = getOptions(readProperties(file: params.propertiesFile))
    }
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${projectName} -Denv=${deployEnvironment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}${options}"
    }
}

@NonCPS
def getOptions(props) {
    result = ''
    for (prop in props) {
        result += " -D${prop.key}=${prop.value}"
    }
    result
}

