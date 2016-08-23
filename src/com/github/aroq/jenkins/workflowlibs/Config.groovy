package com.github.aroq.jenkins.workflowlibs

def readGroovyConfig(filePath) {
    echo "Groovy config at ${filePath}:"
    def text = readFile(filePath)
    groovyConfig(text)
}

@NonCPS
def groovyConfig(text) {
    return new HashMap<>(ConfigSlurper.newInstance().parse(text))
}
