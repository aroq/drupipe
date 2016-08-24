def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    stage 'config'

    echo '--- CONFIG stage'
    
    dir('library') {
        git url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master'
    }

    def config = (new com.github.aroq.workflowlibs.Config()).readGroovyConfig(params.configFileName)
    config << params

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        docman.info2(config)
    }

}
