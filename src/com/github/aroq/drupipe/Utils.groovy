package com.github.aroq.drupipe

import groovy.json.JsonSlurper

def colorEcho(message, color = null) {
    if (!color) {
        color = 'green'
    }
    switch (color) {
        case 'red':
            color = 31
            break
        case 'green':
            color = 32
            break
        case 'yellow':
            color = 33
            break
        case 'blue':
            color = 34
            break
        case 'magenta':
            color = 35
            break
        case 'cyan':
            color = 36
            break
    }

    wrap([$class: 'AnsiColorBuildWrapper']) {
        echo "\u001B[${color}m${message}\u001B[0m"
    }
}

@NonCPS
List<Stage> processStages(stages) {
    List<Stage> result = []
    for (item in stages) {
        result << processStage(item)
    }
    result
}

@NonCPS
Stage processStage(stage) {
    if (stage instanceof Stage) {
        for (action in stage.actions) {
            values = action.action.split("\\.")
            action.name = values[0]
            action.methodName = values[1]
        }
        stage
    }
    else {
        new Stage(name: stage.key, actions: processPipelineActionList(stage.value))
    }
}

@NonCPS
List processPipelineActionList(actionList) {
    List actions = []
    for (action in actionList) {
        actions << processPipelineAction(action)
    }
    actions
}

@NonCPS
Action processPipelineAction(action) {
    if (action.getClass() == java.lang.String) {
        actionName = action
        actionParams = [:]
    }
    else {
        actionName = action.action
        actionParams = action.params
    }
    values = actionName.split("\\.")
    new Action(name: values[0], methodName: values[1], params: actionParams)
}

@NonCPS
def projectNameByGroupAndRepoName(docrootConfigJson, groupName, repoName) {
    // TODO: Refactor it.
    def gName = groupName.toLowerCase()
    def rName = repoName.toLowerCase()
    def docmanConfig = JsonSlurper.newInstance().parseText(docrootConfigJson)
    def result = ''
    docmanConfig.projects.each { project ->
        def repo = project.value['repo'];
        if (repo) {
            echo "REPO: ${repo.toLowerCase()}"
            echo "GITLAB: ${gName}/${rName}"
            if (repo.toLowerCase().contains("${gName}/${rName}")) {
                result = project.value['name']
            }
        }
    }
    result.toString()
}

def writeEnvFile() {
    sh 'env > env.txt'
    writeFile(file: 'ENV.groovy', text: envConfig(readFile('env.txt')))
    sh 'rm -fR env.txt'
}

def envToMap() {
    sh 'env > env.txt'
    def result = envTextToMap(readFile('env.txt'))
    sh 'rm -fR env.txt'
    result
}

def dumpConfigFile(config, fileName = 'config.dump.groovy') {
    echo "Dumping config file: config.dump.groovy"
    writeFile(file: fileName, text: configToSlurperFile(config))
    sh "cat ${fileName}"
}

@NonCPS
String envConfig(env) {
    def co = new ConfigObject()
    env.split("\r?\n").each {
        co.put(it.substring(0, it.indexOf('=')), it.substring(it.indexOf('=') + 1))
    }
    def sw = new StringWriter()
    co.writeTo(sw)
    sw.toString()
}

@NonCPS
def envTextToMap(env) {
    def result = [:]
    env.split("\r?\n").each {
        result.put(it.substring(0, it.indexOf('=')), it.substring(it.indexOf('=') + 1))
    }
    result
}

@NonCPS
String configToSlurperFile(config) {
    def co = new ConfigObject()
    skipConfigKeys = ['action', 'sources', 'sourcesList', 'stage']
    config.each { entry ->
        if (!skipConfigKeys.contains(entry.key)) {
            co.put(entry.key, entry.value)
        }
    }
    def sw = new StringWriter()
    co.writeTo(sw)
    sw.toString()
}

String getJenkinsFolderName(String buildUrl) {
    (buildUrl =~ $/job/(.+)/job/(.+)/.*/$)[ 0 ] [ 1 ]
}

String getJenkinsJobName(String buildUrl) {
    (buildUrl =~ $/job/(.+)/job/(.+)/.*/$)[ 0 ] [ 2 ]
}

@NonCPS
def getMothershipProjectParams(config, json) {
    def projects = JsonSlurper.newInstance().parseText(json).projects
    echo "MOTHERSHIP PROJECTS: ${projects}"
    projects[config.jenkinsFolderName] ? projects[config.jenkinsFolderName] : [:]
}

def loadLibrary(script, params) {
    script.executePipelineAction([
        action: 'Source.add',
        params: [
            source: [
                name: 'library',
                type: 'git',
                path: 'library',
                url: params.drupipeLibraryUrl,
                branch: params.drupipeLibraryBranch,
                refType: params.drupipeLibraryType,
            ],
        ],
    ], params)
}

return this
