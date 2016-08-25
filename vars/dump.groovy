def call(params, String dumpName = null) {
    if (dumpName) {
        echo "Dumping ${dumpName} values:"
    }
    for (item in params) {
        echo "${item.key} = ${item.value}"
    }
}