import com.github.aroq.drupipe.Stage

def call(Stage stageInstance, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    utils = new com.github.aroq.drupipe.Utils()

    stage(stageInstance.name) {
        gitlabCommitStatus(stageInstance.name) {
            params << ['stage': stageInstance]
            params << utils.executeActionList(stageInstance.actions, params)
        }
    }
}
