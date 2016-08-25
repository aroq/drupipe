def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.p
    params.remove('p')

    def config = [:]

    if (params.configFileName) {
        config = (new com.github.aroq.workflowlibs.Config()).readGroovyConfig(params.configFileName)
    }
    config << params
    dump(config)

    config
}
