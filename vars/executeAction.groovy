import com.github.aroq.drupipe.Action

def call(Action action, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    def actionParams = [:]

    try {
        String drupipeStagename
        if (params.stage) {
            drupipeStageName = "${params.stage.name}"
        }
        else {
            drupipeStageName = 'config'
        }

        echoDelimiter("-----> Stage: ${drupipeStageName} | Action name: ${action.fullName} start <-")
        utils = new com.github.aroq.drupipe.Utils()
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

        debugLog(actionParams, actionParams, "${action.fullName} action params")
        // TODO: configure it:
        def actionFile = null
        if (params.sourcesList) {
            for (i = 0; i < params.sourcesList.size(); i++) {
                source = params.sourcesList[i]
                fileName = sourcePath(params, source.name, 'pipelines/actions/' + action.name + '.groovy')
                debugLog(actionParams, fileName, "Action file name to check")
                // To make sure we only check fileExists in Heavyweight executor mode.
                echo "PARAMS BLOCK: ${params.block}"
                if (params.block?.nodeName && fileExists(fileName)) {
                    actionFile = load(fileName)
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

boolean isCollectionOrList(object) {
    object instanceof java.util.Collection || object instanceof java.util.List || object instanceof java.util.LinkedHashMap || object instanceof java.util.HashMap
}
