def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.p
    params.remove('p')


    def config = (new com.github.aroq.workflowlibs.Config()).readGroovyConfig(params.configFileName)
    config << params
    wrap([$class: 'AnsiColorBuildWrapper']) {
        echo "\u001B[31mDumping config values...START"
        for (item in config) {
            echo "${item.key} = ${item.value}"
        }
        echo "Dumping config values...END\u001B[0m"
    }

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        docman.info2(config)
    }

    config
}
