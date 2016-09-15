import groovy.transform.Field

import com.github.aroq.workflowlibs.ConfigContainer

@Field
ConfigContainer configContainer

def call() {
    echo "getConfig()"
    if (!this.configContainer) {
        echo "Create new Config Container"
        this.configContainer = new ConfigContainer()
    }
    return this.configContainer
}

