import groovy.transform.Field

import com.github.aroq.workflowlibs.ConfigContainer

@Field
ConfigContainer configContainer

@Field
Integer callCount = 0

def call() {
    callCount++
    echo "Call count: ${callCount}"
    echo "getConfig()"
    jsonDump(configContainer, 'before creation')
    if (!configContainer) {
        echo "Create new Config Container"
        configContainer = new ConfigContainer(params: [asfasdf: 'asdfadfa'])
        this.configContainer.params = [test2: 'test2']
        this.configContainer.params['test4'] = 'test4'
        jsonDump(configContainer, 'after creation')
    }
    return configContainer
}

