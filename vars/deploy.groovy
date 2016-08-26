def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
//        HashMap pipeline = [
//                'init' : ['Config.perform'],
//                'build': ['Docman.deploy', 'Docman.info'],
//                'ops'  : ['Druflow.deployFlow']
//        ]
        pipeline = jsonParse('{"init": ["Config.perform"], "build": ["Docman.deploy", "Docman.info"]}')

        for (s in pipeline) {
            echo "Class: ${s.value.getClass()}"
//            params << executeStage(s.key) {
//                p = params
//                actions = s.value
//            }
        }

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

@NonCPS
def jsonParse(String jsonText) {
    slurper = new groovy.json.JsonSlurper()
    slurper.parseText(jsonText)
}

@NonCPS
List<List<Object>> get_map_entries(map) {
    map.collect {k, v -> [k, v]}
}
