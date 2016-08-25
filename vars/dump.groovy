def call(params, String dumpName = null) {
    utils = new com.github.aroq.workflowlibs.Utils()
    if (dumpName) {
        utils.colorEcho "Dumping ${dumpName} values:"
    }

    utils.colorEcho collectParams(params)
}

@NonCPS
def collectParams(params) {
    def String result
    for (item in params) {
        result += "${item.key} = ${item.value}\r\n"
    }
    result
}