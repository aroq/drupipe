package com.github.aroq.jenkins.workflowlibs

def readGroovyConfig(filePath) {
    def text = readFile(filePath)
    echo "Groovy config at ${filePath}:"
    echo text
    groovyConfig(text)
}

@NonCPS
def groovyConfig(text) {
    return new HashMap<>(ConfigSlurper.newInstance().parse(text))
}
