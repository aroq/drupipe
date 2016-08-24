def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.params

    wrap([$class: 'AnsiColorBuildWrapper']) {
        stage "\u001B[31mINIT\u001B[0m"
    }

    dir('library') {
        git url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master'
    }

    config = configHelper {
        params = params
    }

    config
}
