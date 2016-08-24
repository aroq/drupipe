def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.paramsTest

    echo 'Config'

    params.each {k, v ->
        echo v
    }

    def config = (new com.github.aroq.workflowlibs.Config()).readGroovyConfig(params.configFileName)
    config << params

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        docman.info2(config)
    }

    config
}
