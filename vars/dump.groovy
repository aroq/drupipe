def call(params, String dumpName = null) {
    utils = new com.github.aroq.workflowlibs.Utils()
    if (dumpName) {
        utils.colorEcho "Dumping ${dumpName} values:"
    }
    for (item in params) {
        utils.colorEcho "${item.key} = ${item.value}"
    }
}