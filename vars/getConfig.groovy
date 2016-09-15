import groovy.transform.Field

@Field
def configContainer = [:]

def call() {
    if (this.configContainer.size() == 0) {
        this.configContainer = new com.github.aroq.workflowlibs.ConfigContainer()
    }
    this.configContainer
}

