def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.p
    params.p = null

    stage 'Init'

    dir('library') {
        git url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master'
    }

    config = configHelper {
        p = params
    }


    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        config.initDocman = false
        docman.info(config)
    }

    config
}
