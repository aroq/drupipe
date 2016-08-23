package com.github.aroq.jenkins.workflowlibs

def readGroovyConfig(filePath) {
    echo "filePath: ${filePath}"
    config = ConfigSlurper.newInstance().parse(readFile(file: filePath))
    echo config
    config
}
