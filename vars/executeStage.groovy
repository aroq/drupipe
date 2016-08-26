def call(name, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    params << params.p
    params.remove('p')

    stage name

    try {
        for (action in params.actions) {
//            def values = action.split("\\.")
//            def actionInstance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.${values[0]}", true, false )?.newInstance()
//            def methodName = values[1]
//            dump(params, "${action} action params")
//            actionResult = actionInstance."$methodName"(params)
//            if (actionResult) {
//                params << actionResult
//            }
//            dump(params, "${action} action result")
        }
        params.remove('actions')
        params
    }
    catch (err) {
        echo "Action ${action} is not exists or error in action."
        echo err.toString()
        throw err
    }
}
