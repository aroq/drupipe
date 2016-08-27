def call(name, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    try {
        action = params.action
        dump(params << action.params, "${action.name} action params")
        echo "PWD: ${pwd()}"
        // TODO: configure it:
        fileName = 'docroot/config/pipelines/actions/' + action.name + '.groovy'
        echo "Action file name to check: ${fileName}"
        if (fileExists(fileName)) {
            actionFile = load(fileName)
            actionResult = actionFile."$action.methodName"(params << action.params)
        }
        else {
            try {
                def actionInstance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.actions.${action.name}", true, false )?.newInstance()
                actionResult = actionInstance."$action.methodName"(params << action.params)
            }
            catch (err) {
                echo err.toString()
            }
        }

        if (actionResult) {
            echo "Result type: ${actionResult.getClass()}"
            if (isCollectionOrList(actionResult)) {
                params << actionResult
            }
            else {
                params << ["${action.name}.${action.methodName}": actionResult]
            }
        }
        dump(params, "${action.name} action result")
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
