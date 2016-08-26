def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        HashMap pipeline = [
                'init' : ['Config.perform'],
                'build': ['Docman.deploy', 'Docman.info'],
                'ops'  : ['Druflow.deployFlow']
        ]
        for (s in pipeline) {
            params << executeStage(s.key) {
                p = params
//                actions = stage.value
                actions = ['Config.perform']
            }
//            stage = null
        }
//        pipeline = null
//        params << executeStage('init') {
//            p = params
//            actions = ['Config.perform']
//        }
//
//        params << executeStage('build') {
//            p = params
//            actions = ['Docman.deploy', 'Docman.info']
//        }
//
//        params << executeStage('ops') {
//            p = params
//            actions = ['Druflow.deployFlow']
//        }
    }
}
