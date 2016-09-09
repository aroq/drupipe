def call(params, value, String dumpName = '') {
    if (params.debug) {
        if (value instanceof java.lang.String) {
            echo "${dumpName}: ${value}"
        }
        else {
            jsonDump(value, dumpName)
        }
    }
}

