import groovy.transform.Field

import com.github.aroq.workflowlibs.ConfigContainer

@Field
ConfigContainer configContainer = new ConfigContainer()

def call() {
    if (!this.configContainer == 0) {
//        this.configContainer = new ConfigContainer()
    }
    return this.configContainer
}

