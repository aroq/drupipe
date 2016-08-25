def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        config = initStage {
            p = params
        }

//        params << config

        build {
            p = params
            actions = ['Docman.deploy', 'Docman.info']
        }
    }
}
