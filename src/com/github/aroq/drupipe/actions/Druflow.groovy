package com.github.aroq.drupipe.actions

def deployFlow(params) {
   if (params.executeEnvironment) {
        executeEnvironment = params.executeEnvironment
    }
    else {
        executeEnvironment = environment
    }

    echo "PARAMS DEPLOY FLOW: ${params}"

    executeDruflowCommand(params, [env: executeEnvironment, projectName: params.projectName])
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
    echo "CHECKING DIRS"
    sh 'ls -l docroot'
    sh 'ls -l docroot/config'
    def druflowCommand = prepareDruflowCommand(params, overrides)
    druflowGet(params)
    drupipeShell(druflowCommand, params)
}

def druflowGet(params) {
    if (fileExists(params.druflowDir)) {
        dir(params.druflowDir) {
            deleteDir()
        }

    }
    sh "git clone ${params.druflowRepo} --branch ${params.druflowGitReference} --depth 1 ${params.druflowDir}"
}

def copySite(params) {
    def dbs = []
    if (params.db instanceof java.lang.String) {
        dbs << params.db
    }
    else {
        dbs = params.db
    }
    for (db in dbs) {
        executeDruflowCommand(params, [argument: "'${db} ${params.toEnvironment}'", env: params.executeEnvironment, site: 'default'])
    }
}

def dbBackupSite(params) {
    def dbs = []
    if (params.db instanceof java.lang.String) {
        dbs << params.db
    }
    else {
        dbs = params.db
    }
    for (db in dbs) {
        executeDruflowCommand(params, [argument: db, env: params.executeEnvironment, site: 'default'])
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

