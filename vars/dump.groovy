import com.github.aroq.workflowlibs.Utils

def call(params, String dumpName = null) {
    if (dumpName) {
        Utils.colorEcho "Dumping ${dumpName} values:"
    }
    for (item in params) {
        Utils.colorEcho "${item.key} = ${item.value}"
    }
}