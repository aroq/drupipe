package com.github.aroq.drupipe

import groovy.json.JsonOutput

class DrupipeLogLevel {
    int weight
    String color
}

class DrupipeLogger implements Serializable {

    com.github.aroq.drupipe.Utils utils

    LinkedHashMap<String, DrupipeLogLevel> logLevels

    int logLevelWeight

    def logMessage(String logLevel, String message) {
        if (logLevels[logLevel].weight >= logLevelWeight) {
            utils.colorEcho "[${logLevel}] " + message, logLevels[logLevel].color
        }
    }

    def log(String message) {
        info(message)
    }

    def trace(String message) {
        logMessage('TRACE', message)
    }

    def info(String message) {
        logMessage('INFO', message)
    }

    def debug(String message) {
        logMessage('DEBUG', message)
    }

    def warning(String message) {
        logMessage('WARNING', message)
    }

    def error(String message) {
        logMessage('ERROR', message)
    }

    def collapsedStart(String message) {
        utils.echoMessage '[COLLAPSED-START] ' + message
    }

    def collapsedEnd() {
        utils.echoMessage '[COLLAPSED-END]'
    }

    def jsonDump(params, value, String dumpName = '', force = false) {
        if (debugEnabled(params) || force) {
            if (dumpName) {
                utils.echoMessage "DUMP START - ${dumpName}"
            }
            debug JsonOutput.prettyPrint(JsonOutput.toJson(value))
            if (dumpName) {
                utils.echoMessage "DUMP END - ${dumpName}"
            }
        }
    }

    def debugLog(params, value, dumpName = '', debugParams = [:], path = [:], force = false) {
        if (debugEnabled(params) || force) {
            force = true
            if (path) {
                value = path.inject(value, { obj, prop ->
                    if (obj && obj[prop]) {
                        obj[prop]
                    }
                    else ''
                })
            }

            if (value instanceof CharSequence) {
                debug "${dumpName}: ${value}"
            }
            else {
                if (debugParams?.debugMode == 'json' || params.debugMode == 'json') {
                    jsonDump(params, value, dumpName, force)
                }
                else {
                    dump(params, value, dumpName, force)
                }
            }
        }
    }

    def dump(context, params, String dumpName = '', force = false) {
        if (debugEnabled(context) || force) {
            debug "Dumping ${dumpName}:"
            debug collectParams(params)
        }
    }

    def debugEnabled(params) {
        params.debugEnabled && params.debugEnabled != '0'
    }

    @NonCPS
    def collectParams(params) {
        String result = ''
        for (item in params) {
            result = result + "${item.key} = ${item.value}\r\n"
        }
        result
    }

    def echoDelimiter(String message) {
        if (message) {
            if (message.size() < 80) {
                utils.echoMessage message + '-' * (80 - message.size())
            }
            else {
                utils.echoMessage message
            }
        }
    }


}
