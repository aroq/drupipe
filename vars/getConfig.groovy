import groovy.transform.Field

import com.github.aroq.workflowlibs.ConfigContainer

@Field
ConfigContainer configContainer

def call() {
    echo "getConfig()"
    jsonDump(this.configContainer, 'before creation')
    if (!this.configContainer) {
        echo "Create new Config Container"
        this.configContainer = new ConfigContainer(params: [asfasdf: 'asdfadfa'])
        jsonDump(this.configContainer, 'after creation')
    }
    return this.configContainer
}

