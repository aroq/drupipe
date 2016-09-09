import org.codehaus.groovy.runtime.GStringImpl

def call(params, value, String dumpName = '', debugParams = [:]) {
    if (params.debug && value) {
        if (value instanceof java.lang.String || value instanceof GStringImpl) {
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

