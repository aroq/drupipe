def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        checkout scm

        params << executeStage('init') {
            p = params
            actions = ['Library.perform', 'Config.perform']
        }

        params << executeStage('seed') {
            p = params
            actions = ['JobDslSeed.perform']
        }
    }
}
