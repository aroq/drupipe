package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
   if (params.deployFlowEnvironment) {
        executeEnvironment = params.deployFlowEnvironment
    }
    else {
        executeEnvironment = environment
    }
    if (params.projectName) {
        deployProjectName = params.projectName
    }
    else {
        deployProjectName = projectName
    }

    if (executeEnvironment == params.deployFlowConfirm?.environment) {
        timeout(time: 60, unit: 'MINUTES') {
            input params.deployFlowConfirm?.message
        }
    }

    // options = ''
    // if (fileExists(file: params.propertiesFile)) {
        // options = getOptions(readProperties(file: params.propertiesFile))
    // }

    // druflowGet(params)

    // sh "cd ${params.druflowDir} && ./gradlew app -Ddebug=${debugFlag()} -DprojectName=${deployProjectName} -Denv=${executeEnvironment} -DexecuteCommand=${params.executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}${options}"

    executeDruflowCommand(params, '', [env: executeEnvironment, projectName: deployProjectName])
}

def prepareDruflowCommandParams(params, overrides = [:]) {
    def commandParams = params
    defaultParams = [
        debug: debugFlag(),
        executeCommand: commandParams.executeCommand,
        workspace: params.workspace,
        // TODO: review this parameter handling.
        docrootDir: docrootDir,
    ]
    commandParams << defaultParams
    commandParams << overrides
}

def prepareDruflowCommand(params, argument, overrides) {
    commandParams = prepareDruflowCommandParams(params, overrides)

    options = ''
    options += getOptions(commandParams)
    if (commandParams.propertiesFile && fileExists(file: commandParams.propertiesFile)) {
        options += getOptions(readProperties(file: commandParams.propertiesFile))
    }

    "cd ${params.druflowDir} && ./gradlew app ${argument} ${options}"
}

def executeDruflowCommand(params, argument = '', overrides = [:]) {
    def druflowCommand = prepareDruflowCommand(params, argument, overrides)
    druflowGet(params)
    sh(druflowCommand)
}

def copySite(params) {
    executeDruflowCommand(params, params.db, [env: params.executeEnvironment, projectName: deployProjectName])
    // druflowGet(params)

    // sh "cd druflow && ./gradlew app -Ddebug=${debugFlag()} -Dsite=default -Denv=${params.executeEnvironment} -Dargument='${params.db} ${params.toEnvironment}' -DexecuteCommand=dbCopyAC -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}"
}

def dbBackupSite(params) {
    executeDruflowCommand(params, params.db, [env: params.executeEnvironment, projectName: deployProjectName])
    // druflowGet(params)

    // sh "cd druflow && ./gradlew app -Ddebug=${debugFlag()} -Dsite=default -Denv=${params.executeEnvironment} -Dargument=${params.db} -DexecuteCommand=dbBackupSite -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}"
}

def druflowGet(params) {
    dir(params.druflowDir) {
        git params.druflowRepo
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

def debugFlag() {
    params.debugEnabled && params.debugEnabled != '0' ? '1' : '0'
}

