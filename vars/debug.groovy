def call(params, String dumpName = '') {
    if (params.debug) {
        colorEcho "Dumping ${dumpName}:"
        colorEcho collectParams(params)
    }
}

