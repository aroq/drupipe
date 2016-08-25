def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.p
    params.p = null

    stage 'Init'

    // Set global variables.
    env.WORKSPACE = pwd()

    dir('library') {
        git url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master'
    }

    config = configHelper {
        p = params
    }
     config.workspace = pwd()

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        config.initDocman = false
        docman.info(config)
    }

    config
}
