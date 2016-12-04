package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
   if (params.executeEnvironment) {
        executeEnvironment = params.executeEnvironment
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

    executeDruflowCommand(params, [env: executeEnvironment, projectName: deployProjectName])
}

def prepareDruflowCommandParams(params, overrides = [:]) {
    defaultParams = [
        debug: debugFlag(),
        executeCommand: params.executeCommand,
        workspace: params.workspace,
        // TODO: review this parameter handling.
        docrootDir: docrootDir,
    ]
    commandParams = defaultParams
    commandParams << overrides
}

def prepareDruflowCommand(params, overrides) {
    commandParams = prepareDruflowCommandParams(params, overrides)

    options = ''
    options += getOptions(commandParams)
    if (params.propertiesFile && fileExists(file: params.propertiesFile)) {
        options += getOptions(readProperties(file: params.propertiesFile))
    }

    "cd ${params.druflowDir} && ./gradlew app ${options}"
}

def executeDruflowCommand(params, overrides = [:]) {
    def druflowCommand = prepareDruflowCommand(params, overrides)
    druflowGet(params)
    sh(druflowCommand)
}

def druflowGet(params) {
    dir(params.druflowDir) {
        git params.druflowRepo
    }
}

def copySite(params) {
    def dbs = []
    if (params.db instanceof java.lang.String) {
        echo "SINGLE DB"
        dbs << params.db
    }
    else {
        echo "MULTIPLE DBS"
        dbs << params.db
    }
    for (db in dbs) {
        executeDruflowCommand(params, [argument: "'${db} ${params.toEnvironment}'", env: params.executeEnvironment, site: 'default'])
    }
}

def dbBackupSite(params) {
    executeDruflowCommand(params, [argument: params.db, env: params.executeEnvironment, site: 'default'])
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

