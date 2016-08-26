import groovy.json.*

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

    echo JsonOutput.prettyPrint(JsonOutput.toJson(pipeline))

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

        for (action in item.value) {
            if (action.getClass() == java.lang.String) {
                def values = action.split("\\.")
                actions << new com.github.aroq.workflowlibs.Action(name: values[0], methodName: values[1])
            }
            else {
                def values = action.action.split("\\.")
                actions << new com.github.aroq.workflowlibs.Action(name: values[0], methodName: values[1], params: actions.params)
            }
        }
        result << new com.github.aroq.workflowlibs.Stage(name: item.key, actionList: actions)
    }
    result
}
