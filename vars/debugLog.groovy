def call(params, value, dumpName = '', debugParams = [:]) {
    echo "debugLog"
    if (params.debugEnabled) {
        echo "debugEnabled"
        if (value instanceof java.lang.String) {
            echo "debugEnabledString"
            echo "${dumpName}: ${value}"
        }
        else {
            echo "debugEnabledNotString"
            if (debugParams?.debugMode == 'json' || params.debugMode == 'json') {
                jsonDump(value, dumpName)
            }
            else {
                dump(value, dumpName)
            }
        }
    }
}

