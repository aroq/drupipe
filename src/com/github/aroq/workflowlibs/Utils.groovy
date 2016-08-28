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
        actions = processPipelineActionList(item.value)
        result << new com.github.aroq.workflowlibs.Stage(name: item.key, actionList: actions)
    }
    result
}

@NonCPS
def processPipelineActionList(actionList) {
    List actions = []
    for (action in actionList) {
        actions << processPipelineAction(action)
    }
    actions
}

@NonCPS
def processPipelineAction(action) {
    if (action.getClass() == java.lang.String) {
        actionName = action
        actionParams = [:]
    }
    else {
        actionName = action.action
        actionParams = action.params
    }
    values = actionName.split("\\.")
    new com.github.aroq.workflowlibs.Action(name: values[0], methodName: values[1], params: actionParams)
}

@NonCPS
def processSources(sources) {
    sources.values()
}

return this
