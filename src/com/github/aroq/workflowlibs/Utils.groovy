package com.github.aroq.workflowlibs

def colorEcho(message, color = null) {
    if (!color) {
        color = 'green'
    }
    else {
        switch (color) {
            case 'red':
                color = 31
                break
            case 'green':
                color = 32
                break
            case 'yellow':
                color = 33
                break
            case 'blue':
                color = 34
                break
            case 'magenta':
                color = 35
                break
            case 'cyan':
                color = 36
                break
        }
    }

    wrap([$class: 'AnsiColorBuildWrapper']) {
        echo "\u001B[${color}m${message}\u001B[0m"
    }
}

return this
