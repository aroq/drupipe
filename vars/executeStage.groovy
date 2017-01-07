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

	echo "STAGE INSTNCE: ${stageInstance}"

    stage(stageInstance.name) {
        gitlabCommitStatus(stageInstance.name) {
            params << ['stage': stageInstance]

            params << executeActionList(stageInstance.actionList) {
                p = params
            }
        }
    }
}
