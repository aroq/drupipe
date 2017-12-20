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

    boolean checkLogLevelWeight(String logLevel) {
        logLevels[logLevel].weight >= logLevelWeight
    }

    def logMessage(String logLevel, String message) {
        if (checkLogLevelWeight(logLevel)) {
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

    def jsonDump(value, String dumpName = '', String logLevel = 'DEBUG') {
        if (checkLogLevelWeight(logLevel)) {
            if (dumpName) {
                utils.echoMessage "DUMP START - ${dumpName}"
            }
            utils.echoMessage JsonOutput.prettyPrint(JsonOutput.toJson(value))
            if (dumpName) {
                utils.echoMessage "DUMP END - ${dumpName}"
            }
        }
    }

    def debugLog(params, value, dumpName = '', debugParams = [:], path = [:], String logLevel = 'DEBUG') {
        if (checkLogLevelWeight(logLevel)) {
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
                    jsonDump(value, dumpName, logLevel)
                }
                else {
                    dump(value, dumpName, logLevel)
                }
            }
        }
    }

    def dump(params, String dumpName = '', String logLevel = 'DEBUG') {
        logMessage(logLevel, "Dumping ${dumpName}:")
        logMessage(logLevel, collectParams(params))
//        if (debugEnabled(context) || force) {
//            debug "Dumping ${dumpName}:"
//            debug collectParams(params)
//        }
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
