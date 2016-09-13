def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    pipelineMultibranch {
        p = params
        actions = {

        }
    }

}
