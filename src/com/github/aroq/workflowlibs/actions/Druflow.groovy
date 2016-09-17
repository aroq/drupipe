package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    debugLog(params, params, 'Deploy Flow')

    options = ' '
    if (fileExists(file: params.propertiesFile)) {
        props = readProperties file: params.propertiesFile
        if (props.tag) {
            options += "-Dtag=${props.tag}"
        }
        echo "Properties: ${getOptions(props)}"
    }
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${projectName} -Denv=${environment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir} ${options}"
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

