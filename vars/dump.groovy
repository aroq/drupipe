def call(params, String dumpName = '') {
    colorEcho "Dumping ${dumpName} values:"
    colorEcho collectParams(params)
}

@NonCPS
def collectParams(params) {
    def String result = ''
    for (item in params) {
        result = result + "${item.key} = ${item.value}\r\n"
    }
    result
}