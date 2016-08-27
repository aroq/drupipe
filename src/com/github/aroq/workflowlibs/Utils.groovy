package com.github.aroq.workflowlibs

def colorEcho(message, color = null) {
    if (!color) {
        color = 'green'
    }
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

    wrap([$class: 'AnsiColorBuildWrapper']) {
        echo "\u001B[${color}m${message}\u001B[0m"
    }
}

@NonCPS
def processPipeline(pipeline) {
    List<com.github.aroq.workflowlibs.Stage> result = []
    for (item in pipeline) {
        List actions = []
        for (action in item.value) {
            actions << processPipelineAction(action)
        }
        result << new com.github.aroq.workflowlibs.Stage(name: item.key, actionList: actions)
    }
    result
}

@NonCPS
def processPipelineAction(action) {
    if (action.getClass() == java.lang.String) {
        def values = action.split("\\.")
        new com.github.aroq.workflowlibs.Action(name: values[0], methodName: values[1])
    }
    else {
        def values = action.action.split("\\.")
        new com.github.aroq.workflowlibs.Action(name: values[0], methodName: values[1], params: action.params)
    }
}

return this
