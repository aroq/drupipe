def call(params, String dumpName = '') {
//    utils = new com.github.aroq.workflowlibs.Utils()
    colorEcho "Dumping ${dumpName} values:"
    colorEcho collectParams(params)
}

@NonCPS
def collectParams(params) {
    def String result = 'params - '
    for (item in params) {
        result = result + "${item.key} = ${item.value}\r\n"
    }
    result
}