package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Druflow extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def operations() {
        deployFlow()
    }

    def deployFlow() {
        def executeEnvironment = action.params.executeEnvironment ? action.params.executeEnvironment : context.environment
        executeDruflowCommand([env: executeEnvironment, projectName: context.projectName])
    }

    def deploy() {
        def site = action.params.site ? action.params.site : 'default'
        executeDruflowCommand([argument: "tags/${action.params.reference}", site: site, env: context.environment, projectName: context.projectName])
    }

    def executeDruflowCommand(overrides = [:]) {
        def defaultParams = [
            debug: debugFlag(),
            executeCommand: action.params.executeCommand,
            workspace: context.workspace,
            // TODO: review this parameter handling.
            docrootDir: action.params.docrootDir ? action.params.docrootDir : context.docrootDir,
            docrootConfigDir: utils.sourceDir(context, 'project'),
        ]
        // TODO: review it.
        if (context.operationsMode) {
            defaultParams['flowType'] = context.operationsMode
        }
        else {
            defaultParams['flowType'] = 'full'
        }
        def commandParams = defaultParams
        commandParams << overrides

        def options = ''
        options += getOptions(commandParams)
        if (action.params.propertiesFile && script.fileExists(file: action.params.propertiesFile)) {
            options += getOptions(getProperties())
        }

        def druflowCommand = "cd ${action.params.druflowDir} && ./gradlew app ${options}"

        druflowGet()

        script.drupipeShell(druflowCommand, context)
    }

    def druflowGet() {
        if (script.fileExists(action.params.druflowDir)) {
            utils.removeDir(action.params.druflowDir, context)
        }
        script.drupipeShell("git clone ${action.params.druflowRepo} --branch ${action.params.druflowGitReference} --depth 1 ${action.params.druflowDir}", context)
    }

    def copySite() {
        for (db in getDbs()) {
            executeDruflowCommand([argument: "'${db} ${action.params.toEnvironment}'", env: action.params.executeEnvironment, site: 'default'])
        }
    }

    def dbBackupSite() {
        for (db in getDbs()) {
            executeDruflowCommand([argument: db, env: action.params.executeEnvironment, site: 'default'])
        }
    }

    def getGitRepo() {
        def executeEnvironment = action.params.executeEnvironment ? action.params.executeEnvironment : context.environment
        executeDruflowCommand([env: executeEnvironment, projectName: context.projectName])
    }

    def getProperties() {
        script.readProperties(file: action.params.propertiesFile)
    }

    def getDbs() {
        def dbs = []
        if (action.params.db instanceof java.lang.String) {
            dbs << action.params.db
        }
        else {
            dbs = action.params.db
        }
        dbs
    }

    @NonCPS
    def getOptions(props) {
        def result = ''
        for (prop in props) {
            result += " -D${prop.key}=${prop.value}"
        }
        result
    }

    def debugFlag() {
        context.debugEnabled && context.debugEnabled != '0' ? '1' : '0'
    }
}


