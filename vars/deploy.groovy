def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        c = initStage {
            p = params
        }

        build {
            p = params
            config = c
            actions = ['Docman.deploy', 'Docman.info2']
        }
    }
}
