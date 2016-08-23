package com.github.aroq.jenkins.workflowlibs

def readGroovyConfig(text) {
    echo "text: ${text}"
    config = groovyConfig(text)
    config
}

@NonCPS
def groovyConfig(text) {
    new groovy.json.JsonSlurper().parseText(json)
    ConfigSlurper.newInstance().parse(text)
}
