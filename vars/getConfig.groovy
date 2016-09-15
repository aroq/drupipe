import groovy.transform.Field

import com.github.aroq.workflowlibs.ConfigContainer

@Field
ConfigContainer configContainer

def call() {
    echo "getConfig()"
    if (this.configContainer?.getParams()) {
        jsonDump(this.configContainer, 'before creation')
        echo "Create new Config Container"
        this.configContainer = new ConfigContainer(params: [asfasdf: 'asdfadfa'])
        jsonDump(this.configContainer, 'after creation')
    }
    return this.configContainer
}

