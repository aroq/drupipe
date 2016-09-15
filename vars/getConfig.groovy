import groovy.transform.Field

@Field
def configContainer = [:]

def call() {
    if (this.config.size() == 0) {
//        this.configContainer = new com.github.aroq.workflowlibs.actions.Config()
    }
    this.configContainer
}

