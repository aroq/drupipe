def call(params, value, dumpName = '', debugParams = [:]) {
    if (params.debugEnabled) {
        if (value instanceof java.lang.String) {
            echo "${dumpName}: ${value}"
        }
        else {
            if (debugParams?.debugMode == 'json' || params.debugMode == 'json') {
                jsonDump(value, dumpName)
            }
            else {
                dump(value, dumpName)
            }
        }
    }
}

