def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        properties(
            [
                [$class: 'GitLabConnectionProperty', gitLabConnection: 'Gitlab'],
            ]
        )

        echo "Brach: ${env.gitlabSourceBranch}"
    }
}
