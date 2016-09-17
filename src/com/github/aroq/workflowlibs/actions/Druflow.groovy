package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    debugLog(params, params, 'Deploy Flow')
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        props = readProperties file: 'docroot/master/version.properties'
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${projectName} -Denv=${environment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir} -Dtag=${props.tag}"
    }
}

