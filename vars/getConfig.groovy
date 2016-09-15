import groovy.transform.Field

@Field
def config = [:]

def call() {
    if (this.config.size() == 0) {
        this.config = new com.github.aroq.workflowlibs.actions.Config()
    }
    this.config
}

