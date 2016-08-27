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
        for (action in actionList) {
            params << executeAction(action) {
                p = params
            }
        }
        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }

    params
}
