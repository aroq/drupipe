package com.github.aroq.workflowlibs

def colorEcho(String message, color = null) {
    wrap([$class: 'AnsiColorBuildWrapper']) {
        stage "\u001B[31m${message}\u001B[0m"
    }
}

return this
