def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    executePipeline {
        checkoutSCM = true
        pipeline = [
            'init' : ['Library.perform', 'Config.perform'],
            'seed' : ['JobDslSeed.perform'],
        ]
        p = params
    }
}
