import com.github.aroq.workflowlibs.Action

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
        echoDelimiter "-----> Action name: ${action.fullName} start <-"
        utils = new com.github.aroq.workflowlibs.Utils()
        actionParams << params
        if (!action.params) {
            action.params = [:]
        }
        actionParams << ['action': action]
        defaultParams = [:]
        for (actionName in [action.name, action.fullName]) {
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
                if (fileExists(fileName)) {
                    actionFile = load(fileName)
                    actionResult = actionFile."$action.methodName"(actionParams)
                }
            }
        }
        if (!actionFile) {
            try {
                def actionInstance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.actions.${action.name}", true, false )?.newInstance()
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
                params << ["${action.name}.${action.methodName}": actionResult]
            }
        }
        debugLog(actionParams, params, "${action.fullName} action result")
        echoDelimiter "-----> Action name: ${action.fullName} end <-"
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
