def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.params
    params.remove('p')

    echo 'Config'

    for (item in params) {
        echo "${item.key} = ${item.value}"
    }

    def config = (new com.github.aroq.workflowlibs.Config()).readGroovyConfig(params.configFileName)
    config << params

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        docman.info2(config)
    }

    config
}
