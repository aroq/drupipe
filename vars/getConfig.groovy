import groovy.transform.Field

import com.github.aroq.workflowlibs.ConfigContainer

@Field
ConfigContainer configContainer

@Field
Integer callCount = 0

def call() {
    this.callCount++
    echo "Call count: ${this.callCount}"
    echo "getConfig()"
    jsonDump(this.configContainer, 'before creation')
    if (!this.configContainer) {
        echo "Create new Config Container"
        this.configContainer = new ConfigContainer(params: [asfasdf: 'asdfadfa'])
        this.configContainer.params = [test2: 'test2']
        this.configContainer.params.test5 = 'test5'
        jsonDump(this.configContainer, 'after creation')
    }
    return this.configContainer
}

