def call(name, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    params << params.p
    params.remove('p')

    stage s.name

    try {
        for (action in s.actionList) {
            def values = action.split("\\.")
            def actionInstance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.${values[0]}", true, false )?.newInstance()
            def methodName = values[1]
            dump(params, "${action} action params")
            actionResult = actionInstance."$methodName"(params)
            if (actionResult) {
                params << actionResult
            }
            dump(params, "${action} action result")
        }
        params.remove('actions')
        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }
}
