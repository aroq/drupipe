package com.github.aroq.drupipe

class Action implements Serializable {
    String action
    String name
    String methodName
    HashMap params = [:]
    def script

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute() {
        def actionParams = [:]

        try {
            def utils = new com.github.aroq.drupipe.Utils()
            String drupipeStagename
            if (params.stage) {
                drupipeStageName = "${params.stage.name}"
            }
            else {
                drupipeStageName = 'config'
            }

            utils.echoDelimiter("-----> Stage: ${drupipeStageName} | Action name: ${action.fullName} start <-")
            actionParams << params
            if (!action.params) {
                action.params = [:]
            }
            actionParams << ['action': action]
            defaultParams = [:]
            for (actionName in [action.name, action.name + '_' + action.methodName]) {
                if (actionName in params.actionParams) {
                    defaultParams << params.actionParams[actionName]
                }
            }
            actionParams << params
            actionParams << defaultParams << action.params

            utils.debugLog(actionParams, actionParams, "${action.fullName} action params")
            // TODO: configure it:
            def actionFile = null
            if (params.sourcesList) {
                for (i = 0; i < params.sourcesList.size(); i++) {
                    source = params.sourcesList[i]
                    fileName = sourcePath(params, source.name, 'pipelines/actions/' + action.name + '.groovy')
                    utils.debugLog(actionParams, fileName, "Action file name to check")
                    // To make sure we only check fileExists in Heavyweight executor mode.
                    if (params.block?.nodeName && script.fileExists(fileName)) {
                        actionFile = script.load(fileName)
                        actionResult = actionFile."$action.methodName"(actionParams)
                    }
                }
            }
            if (!actionFile) {
                try {
                    def actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${action.name}", true, false )?.newInstance()
                    actionResult = actionInstance."$action.methodName"(actionParams)
                }
                catch (err) {
                    echo err.toString()
                    throw err
                }
            }

            if (actionResult && actionResult.returnConfig) {
                if (isCollectionOrList(actionResult)) {
                    params << actionResult
                }
                else {
                    // TODO: check if this should be in else clause.
                    params << ["${action.name}.${action.methodName}": actionResult]
                }
            }
            debugLog(actionParams, params, "${action.fullName} action result")
            params.returnConfig = false
            echoDelimiter "-----> Stage: ${drupipeStageName} | Action name: ${action.fullName} end <-"

            params
        }
        catch (err) {
            echo err.toString()
            throw err
        }
    }

}
