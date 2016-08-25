package com.github.aroq.workflowlibs

def colorEcho(message, color = null) {
    wrap([$class: 'AnsiColorBuildWrapper']) {
        echo "\u001B[31m${message}\u001B[0m"
    }
}

return this
