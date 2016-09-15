import groovy.transform.Field

import com.github.aroq.workflowlibs.ConfigContainer

@Field
ConfigContainer configContainer

def call() {
    if (!this.configContainer == 0) {
        this.configContainer = new ConfigContainer()
    }
    this.configContainer
}

