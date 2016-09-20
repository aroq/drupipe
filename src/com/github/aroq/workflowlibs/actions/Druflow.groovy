package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    if (params.deployFlowEnvironment) {
        deployEnvironment = params.deployFlowEnvironment
    }
    else {
        deployEnvironment = environment
    }
    if (params.projectName) {
        deployProjectName = params.projectName
    }
    else {
        deployProjectName = projectName
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
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${deployProjectName} -Denv=${deployEnvironment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}${options}"
    }
}

def copySite(params) {
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        sh "./gradlew app -Ddebug=${debug} -Denv=${params.fromEnvironment} -DtoEnv=${params.toEnvironment} -Dfrom=${params.workspace}/${params.docrootDir} -Dsite=${params.site} -DexecuteCommand=copySite -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}"
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

