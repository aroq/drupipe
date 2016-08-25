package com.github.aroq.workflowlibs

def deployFlow(params) {
    dump(params, 'Deploy Flow')
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
        sh "./gradlew app -Ddebug=${debug} -DprojectName=${projectName} -Denv=${environment} -DexecuteCommand=${executeCommand} -Dworkspace=${params.WORKSPACE} -DdocrootDir=${docrootDir}"
    }
}

