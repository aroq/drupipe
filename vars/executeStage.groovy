def call(Stage stageInstance, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    stage stageInstance.name

    try {
        for (action in stageInstance.actionList) {
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
}
