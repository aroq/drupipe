def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.p
    params.remove('p')


    def config = (new com.github.aroq.workflowlibs.Config()).readGroovyConfig(params.configFileName)
    config << params
    echo "Dumping config values:"
    for (item in config) {
        echo "${item.key} = ${item.value}"
    }


    config
}
