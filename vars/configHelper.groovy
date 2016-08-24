def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.paramsTest

    echo 'Config'

    for (int i = 0; i < params.size(); ++i) {
        echo params[i].value
    }

    for (item in params) {
        echo item.value
    }

    def config = (new com.github.aroq.workflowlibs.Config()).readGroovyConfig(params.configFileName)
    config << params

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        docman.info2(config)
    }

    config
}
