def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.p
    params.p = null

    stage 'Init'

    // Set global variables.
    env.WORKSPACE = pwd()

}
