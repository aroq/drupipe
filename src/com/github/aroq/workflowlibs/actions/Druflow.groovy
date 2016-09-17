package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    debugLog(params, params, 'Deploy Flow')

    options = ' '
    if (fileExists(file: params.propertiesFile)) {
        props = readProperties file: params.propertiesFile
        options += "-Dtag=${props.tag}"
    }
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${projectName} -Denv=${environment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir} ${options}"
    }
}

