def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()
    params << params.p
    params.p = null

    def config = [:]

    if (params.configFileName) {
        config = readGroovyConfig(params.configFileName)
    }
    config << params
    dump(config, 'Main config')

    config
}

def readGroovyConfig(filePath) {
    def text = readFile(filePath)
    groovyConfig(text)
}

@NonCPS
def groovyConfig(text) {
    return new HashMap<>(ConfigSlurper.newInstance().parse(text))
}