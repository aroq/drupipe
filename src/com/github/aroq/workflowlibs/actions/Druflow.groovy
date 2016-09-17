package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    timeout(time: 10, unit: 'MINUTES') {
      input 'Deploy to Production?'
    }

    debugLog(params, params, 'Deploy Flow')

    options = ''
    if (fileExists(file: params.propertiesFile)) {
        options = getOptions(readProperties(file: params.propertiesFile))
    }
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${projectName} -Denv=${environment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}${options}"
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

