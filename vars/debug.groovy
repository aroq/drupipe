def call(params, value, String dumpName = '', debugParams = [:]) {
    if (params.debug) {
        if (value instanceof java.lang.String) {
            echo "${dumpName}: ${value}"
        }
        else {
            if (debugParams?.debugMode == 'json') {
                jsonDump(value, dumpName)
            }
            else {
                dump(value, dumpName)
            }
        }
    }
}

