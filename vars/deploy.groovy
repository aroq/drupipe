def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()


    node {
        HashMap pipeline = [
                'init' : ['actions': ['Config.perform']],
                'build': ['actions': ['Docman.deploy', 'Docman.info']],
                'ops'  : ['actions': ['Druflow.deployFlow']]
        ]
        for (stage in pipeline) {
            params << executeStage(stage.key) {
                p = params
                actions = stage.value['actions']
            }
            stage = null
        }
        pipeline = null
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
