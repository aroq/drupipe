def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        initResult = executeStage('init') {
            p = params
            actions = ['Library.perform', 'Config.perform']
        }
        dump(initResult, "init result")

        params << executeStage('build') {
            p = params
            actions = ['Docman.deploy', 'Docman.info']
        }

        params << executeStage('ops') {
            p = params
            actions = ['Druflow.deployFlow']
        }
    }
}
