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

        build job: 'development', parameters: [string(name: 'executeCommand', value: 'deployFlow'), string(name: 'projectName', value: 'common'), string(name: 'environment', value: 'dev'), string(name: 'debug', value: '0'), string(name: 'simulate', value: '0'), string(name: 'docrootDir', value: 'docroot'), string(name: 'config_repo', value: 'http://gitlab/drucon2016/config.git'), string(name: 'type', value: 'branch'), string(name: 'version', value: 'develop'), string(name: 'force', value: '0'), string(name: 'skip_stage_build', value: '0'), string(name: 'skip_stage_operations', value: '0'), string(name: 'skip_stage_test', value: '0')]
    }
}
