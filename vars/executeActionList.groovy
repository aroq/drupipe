def call(actionList, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    try {
        utils = new com.github.aroq.drupipe.Utils()
        for (action in actionList) {
            params << utils.executeAction(action, params)
        }
        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }

    params
}
