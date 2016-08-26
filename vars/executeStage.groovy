def call(name, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    stage params.stage.name

    try {
        for (action in params.stage.actionList) {
            dump(params, "${action.name} action params")
            fileName = 'docroot/config/pipelines/actions/' + action.name
            if (fileExists(fileName)) {
                actionFile = load(fileName)
                actionResult = actionFile."$action.methodName"
            }
            else {
                def actionInstance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.actions.${action.name}", true, false )?.newInstance()
                actionResult = actionInstance."$action.methodName"(params << action.params)
            }

            if (actionResult) {
                params << actionResult
            }
            dump(params, "${action.name} action result")
        }
        params.remove('stage')
        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }
}
