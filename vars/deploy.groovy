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

        pp = pipelineParse(pipeline)

        for (int i = 0; i < pp.size(); i++) {
            params.stage = pp[i]
            params << executeStage(pp[i].name) {
                p = params
            }
        }

        jsonPipeline = jsonParse('{"init": ["Config.perform"], "build": ["Docman.deploy", "Docman.info"]}')

//        for (int i = 0; i < jsonPipeline.size(); i++) {
//            params.stage = jsonPipeline[i]
//            params << executeStage(jsonPipeline[i].name) {
//                p = params
//            }
//        }

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
def pipelineParse(pipeline) {
    List<com.github.aroq.workflowlibs.Stage> result = []
    for (item in pipeline) {
//        actions = []
//        for (action in item.value) {
//            actions << action
//        }
        result << new com.github.aroq.workflowlibs.Stage(name: item.key, actionList: ['Config.perform'])
    }
    result
}

@NonCPS
def jsonParse(String jsonText) {
    slurper = new groovy.json.JsonSlurper()
    json = slurper.parseText(jsonText)
    List<com.github.aroq.workflowlibs.Stage> result = []
    for (item in json) {
//        actions = []
//        for (action in item.value) {
//            actions << action
//        }
        result << new com.github.aroq.workflowlibs.Stage(name: item.key, actionList: ['Config.perform'])

    }
    json = null
    slurper = null
    result
}

