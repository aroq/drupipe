package com.github.aroq.jenkins.workflowlibs

def readGroovyConfig(text) {
    echo "text: ${text}"
    config = groovyConfig(text)
    config
}

@NonCPS
def groovyConfig(text) {
//    new groovy.json.JsonSlurper().parseText(json)
    return new HashMap<>(ConfigSlurper.newInstance().parse(text))
}
