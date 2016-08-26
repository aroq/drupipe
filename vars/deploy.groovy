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
        echo "Pipeline class:${pipeline.getClass()}"

//        for (s in pipeline) {
        for(int i = 0; i < pipeline.size(); i++) {
//            echo "Class: ${s.value.getClass()}"
            echo "Class: ${pipeline[i].getClass()}"

            params << executeStage('init') {
                p = params
                actions = ['Config.perform']
            }
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
    json = slurper.parseText(jsonText)
    result = []
    for (item in json) {
//        actions = []
//        for (action in item.value) {
//            actions << action
//        }
        result << new com.github.aroq.workflowlibs.Stage(name: 'init', actionList: ['Config.perform'])

    }
//    json = null
//    slurper = null
//    return new HashMap <> (result)
    result
}

@NonCPS
List<List<Object>> get_map_entries(map) {
    map.collect {k, v -> [k, v]}
}
