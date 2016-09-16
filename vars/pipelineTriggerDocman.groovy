def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
//        properties(
//            [
//                [$class: 'GitLabConnectionProperty', gitLabConnection: 'Gitlab'],
//            ]
//        )
        properties [pipelineTriggers([]), buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '30')), disableConcurrentBuilds(), [$class: 'GitLabConnectionProperty', gitLabConnection: 'Gitlab'], [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]]

        echo "gitlabSourceBranch: ${env.gitlabSourceBranch}"
        echo "gitlabSourceRepoName: ${env.gitlabSourceRepoName}"
        echo "gitlabSourceNamespace: ${env.gitlabSourceNamespace}"
        echo "gitlabSourceRepoURL: ${env.gitlabSourceRepoURL}"
    }
}
