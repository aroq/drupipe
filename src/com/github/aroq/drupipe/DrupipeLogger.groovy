package com.github.aroq.drupipe

import groovy.json.JsonOutput

class DrupipeLogger implements Serializable {

    com.github.aroq.drupipe.Utils utils

    def log(String message) {
        info(message)
    }

    def trace(String message) {
        utils.colorEcho '[TRACE] ' + message, 'cyan'
    }

    def info(String message) {
        utils.colorEcho '[INFO] ' + message, 'green'
    }

    def debug(String message) {
        utils.colorEcho '[DEBUG] ' + message, 'yellow'
    }

    def warning(String message) {
        utils.colorEcho '[WARNING] ' + message, 'red'
    }

    def error(String message) {
        utils.colorEcho '[ERROR] ' + message, 'magenta'
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
