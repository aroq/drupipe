package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    debugLog(params, params, 'Deploy Flow')
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${projectName} -Denv=${environment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir} -Dtag='stable-2016-09-16-21-00-55'"
    }
}

