def call(params, String dumpName = '') {
    if (params.debug) {
        if (params instanceof java.lang.String) {
            echo "${dumpName}: ${params}"
        }
        else {
            jsonDump(params, dumpName)
        }
    }
}

