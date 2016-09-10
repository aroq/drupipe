import com.github.aroq.workflowlibs.Stage

def call(Stage stageInstance, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    stage stageInstance.name
    params << ['stage': stageInstance]

    params << executeActionList(stageInstance.actionList) {
        p = params
    }

}
