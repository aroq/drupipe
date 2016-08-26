import groovy.json.JsonOutput.*

def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    pipeline = processPipeline(params.pipeline)

    echo prettyPrint(toJson(pipeline))

    node {
        if (params.checkoutSCM) {
            checkout scm
        }
        for (int i = 0; i < pipeline.size(); i++) {
            params.stage = pipeline[i]
            params << executeStage(pipeline[i].name) {
                p = params
            }
        }
    }
    params
}

@NonCPS
def processPipeline(pipeline) {
    List<com.github.aroq.workflowlibs.Stage> result = []
    for (item in pipeline) {
        List<String> actions = []

        if (item.value.getClass() == ArrayList) {
            for (action in item.value) {
                actions << action
            }
        }
        else {
            for (action in item.value['actions']) {
                actions << action
            }
        }
        result << new com.github.aroq.workflowlibs.Stage(name: item.key, actionList: actions)
    }
    result
}
