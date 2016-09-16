def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        properties[pipelineTriggers([]), buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '30')), disableConcurrentBuilds(), [$class: 'GitLabConnectionProperty', gitLabConnection: 'Gitlab'], [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]]

        stage 'trigger'
        gitlabCommitStatus {
            echo "gitlabSourceBranch: ${env.gitlabSourceBranch}"
            echo "gitlabSourceRepoName: ${env.gitlabSourceRepoName}"
            echo "gitlabSourceNamespace: ${env.gitlabSourceNamespace}"
            echo "gitlabSourceRepoURL: ${env.gitlabSourceRepoURL}"

            // TODO: Use docman config to set params.
            switch (env.gitlabSourceBranch) {
                case 'develop':
                    buildJob = 'development'
                    buildEnvironment = 'predev'
                    buildVersionType = 'branch'
                    break
                case 'master':
                    buildJob = 'staging'
                    buildEnvironment = 'dev'
                    buildVersionType = 'branch'
                    break
                case 'state_stable':
                    buildJob = 'stable'
                    buildEnvironment = 'test'
                    buildVersionType = 'tag'
                    break
            }

            build job: buildJob, parameters: [
                string(name: 'executeCommand', value: 'deployFlow'),
                string(name: 'projectName', value: env.gitlabSourceRepoName),
                string(name: 'environment', value: buildEnvironment),
                string(name: 'debug', value: '0'),
                string(name: 'simulate', value: '0'),
                string(name: 'docrootDir', value: 'docroot'),
                string(name: 'config_repo', value: params.configRepo),
                string(name: 'type', value: buildVersionType),
                string(name: 'version', value: env.gitlabSourceBranch),
                string(name: 'force', value: '0'),
                string(name: 'skip_stage_build', value: '0'),
                string(name: 'skip_stage_operations', value: '0'),
                string(name: 'skip_stage_test', value: '0')]
        }
    }
}
