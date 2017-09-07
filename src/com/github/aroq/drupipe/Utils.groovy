package com.github.aroq.drupipe

import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput

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
def projectNameByGroupAndRepoName(script, docrootConfigJson, groupName, repoName) {
    // TODO: Refactor it.
    groupName = groupName.toLowerCase()
    repoName = repoName.toLowerCase()
    docmanConfig = JsonSlurperClassic.newInstance().parseText(docrootConfigJson)
    result = ''
    docmanConfig.projects.each { project ->
        def repo = project.value['repo'];
        if (repo) {
        script.echo "REPO: ${repo.toLowerCase()}"
        script.echo "GITLAB: ${groupName}/${repoName}"
            if (repo.toLowerCase().contains("${groupName}/${repoName}")) {
                result = project.value['name']
            }
        }
    }
    result
}

def envToMap() {
    def result = [:]
    sh 'env > env.txt'
    if (fileExists('env.txt')) {
        result = envTextToMap(readFile('env.txt'))
        sh 'rm -fR env.txt'
    }
    else {
        throw "No env.txt file created."
    }
    result
}

def dumpConfigFile(config, fileName = 'config.dump.groovy') {
    echo "Dumping config file: config.dump.groovy"
    writeFile(file: fileName, text: configToSlurperFile(config))
    sh "cat ${fileName}"
}

@NonCPS
def envTextToMap(env) {
    echo "ENV: ${env}"
    def result = [:]
    env.split("\r?\n").each {
        if (it.indexOf('=') > 0) {
            result.put(it.substring(0, it.indexOf('=')), it.substring(it.indexOf('=') + 1))
        }
    }
    result
}

@NonCPS
String configToSlurperFile(config) {
    def co = new ConfigObject()
    skipConfigKeys = ['action', 'sources', 'loadedSources', 'sourcesList', 'stage', 'pipeline', 'block', 'utils']
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
    def result = (buildUrl =~ $/(job/(.+)/)?job/(.+)/.*/$)
    if (result && result[0] && result[0][2]) {
        return result[0][2]
    }
    else {
        echo "Job not in folder."
        return ""
    }
}

String getJenkinsJobName(String buildUrl) {
    def result = (buildUrl =~ $/(job/(.+)/)?job/(.+)/.*/$)
    if (result && result[0] && result[0][3]) {
        return result[0][3]
    }
    else {
        echo "Empty job name."
        return ""
    }
}

@NonCPS
def getMothershipProjectParams(config, json) {
    def projects = JsonSlurperClassic.newInstance().parseText(json).projects
    projects[config.jenkinsFolderName] ? projects[config.jenkinsFolderName] : [:]
}

def loadLibrary(script, context) {
    if (!context.libraryLoaded) {
        script.drupipeAction([
            action: 'Source.add',
            params: [
                source: [
                    name: 'library',
                    type: 'git',
                    path: 'library',
                    url: context.drupipeLibraryUrl,
                    branch: context.drupipeLibraryBranch,
                    refType: context.drupipeLibraryType,
                ],
            ],
        ], context)
        context.libraryLoaded = true
    }
}

boolean isCollectionOrList(object) {
    object instanceof java.util.Collection || object instanceof java.util.List || object instanceof java.util.LinkedHashMap || object instanceof java.util.HashMap
}

def isEventInNotificationLevels(event, levels) {
    for (level in levels) {
        if (level.name) {
            def level_name = level.name.replace('*', '.*')
            def pattern = ~"^${level_name}\$"
            if (event.level ==~ pattern && event.status in level.status) {
                echo "Notifications: Matched ${event.level} with ${level}"
                return true
            }
            else if (level.name == 'action' && event.level.startsWith(level.name) && event.status in level.status) {
                echo "Notifications: Matched ${event.level} with ${level}"
                return true
            }
        }
    }
    echo "Notifications: Not matched"
    return false
}

@NonCPS
def paramsMarkdownTable(jenkinsParams) {
  def String table = ""

  if (jenkinsParams) {

      table = table + "|Parameter|Value|\n"
      table = table + "|:---|:---|\n"

      jenkinsParams.each {param, value ->
          table = table + "|${param}|`${value}`|\n"
      }
  }

  return table
}

def pipelineNotify(context, event) {

    // Default values
    def colorName = 'RED'
    def colorCode = '#FF0000'
    def subject = "${event.name}\n\nJob '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "##### ${event.name}\n---\n\nJob: [${env.JOB_NAME} (${env.BUILD_NUMBER})](${env.BUILD_URL})"

    // Add job params to build message.
    if (context.jenkinsParams && event.level == 'build') {
        def table = paramsMarkdownTable(context.jenkinsParams)
        summary = summary + "\n\n" + table
    }

    // Add event message.
    if (event.message) {
        summary = summary + "\n\n" + event.message
    }

    // Limit message length to 3500 symbols.
    if (summary.length() > 3500) {
        summary = summary.substring(0, 3500).replaceAll(/\n.*$/, '')
    }

    def details = """<p>Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>"""

    // Override default values based on build status
    if (event.status == 'FAILED') {
        color = 'RED'
        colorCode = '#FF0000'
    } else if (event.status == 'SUCCESSFUL') {
        color = 'GREEN'
        colorCode = '#00FF00'
    } else {
        color = 'YELLOW'
        colorCode = '#FFFF00'
    }

    if (context.job && context.job.notify && context.jenkinsParams.containsKey('mute_notification')) {
        def mute_notification = context.jenkinsParams.mute_notification.split(",")
        for (def i = 0; i < context.job.notify.size(); i++) {
            def config = context.job.notify[i]
            echo "Notifications: Config ${config}"

            if (config in mute_notification) {
                echo "Notifications: Notification to ${config} channel was muted"
                return
            }

            def params = []
            if (context.notification && context.notification[config]) {
                params = context.notification[config]
            }

            if (params.levels && event.level && isEventInNotificationLevels(event, params.levels)) {

                // Send notifications
                if (params.slack && params.slackChannel) {
                    try {
                        echo 'Notifications: Send message to Slack'
                        slackSend (color: colorCode, message: summary, channel: params.slackChannel)
                    }
                    catch (e) {
                        echo 'Notifications: Unable to sent Slack notification'
                    }
                }

                if (params.mattermost && params.mattermostChannel && params.mattermostIcon && params.mattermostEndpoint) {
                    try {
                        if (env.BUILD_USER_ID) {
                            summary = "@${env.BUILD_USER_ID} ${summary}"
                        }
                        echo 'Notifications: Send message to Mattermost'
                        mattermostSend (color: colorCode, message: summary, channel: params.mattermostChannel, icon: params.mattermostIcon, endpoint: params.mattermostEndpoint)
                    }
                    catch (e) {
                        echo 'Notifications: Unable to sent Mattermost notification'
                    }
                }

                // hipchatSend (color: color, notify: true, message: summary)

                if (params.emailExt) {
                    echo 'Notifications: Send email'
                    def to = emailextrecipients([
                        [$class: 'CulpritsRecipientProvider'],
                        [$class: 'DevelopersRecipientProvider'],
                        [$class: 'RequesterRecipientProvider']
                    ])

                    emailext (
                        subject: subject,
                        body: details,
                        to: to,
                        mimeType: 'text/html',
                        attachLog: true,
                    )
                }
            }
        }
    }
}

def sourcePath(params, sourceName, String path) {
    debugLog(params, sourceName, 'Source name')
    if (sourceName in params.loadedSources) {
        params.loadedSources[sourceName].path + '/' + path
    }
}

def sourceDir(params, sourceName) {
    debugLog(params, sourceName, 'Source name')
    if (sourceName in params.loadedSources) {
        params.loadedSources[sourceName].path
    }
}

def debugLog(params, value, dumpName = '', debugParams = [:]) {
    if (params.debugEnabled) {
        if (value instanceof java.lang.String) {
            echo "${dumpName}: ${value}"
        }
        else {
            if (debugParams?.debugMode == 'json' || params.debugMode == 'json') {
                jsonDump(value, dumpName)
            }
            else {
                dump(value, dumpName)
            }
        }
    }
}

def dump(params, String dumpName = '') {
    colorEcho "Dumping ${dumpName}:"
    colorEcho collectParams(params)
}

@NonCPS
def collectParams(params) {
    def String result = ''
    for (item in params) {
        result = result + "${item.key} = ${item.value}\r\n"
    }
    result
}

def echoDelimiter(String message) {
    if (message) {
        if (message.size() < 80) {
            echo message + '-' * (80 - message.size())
        }
        else {
            echo message
        }
    }
}

def jsonDump(value, String dumpName = '') {
    if (dumpName) {
        echo dumpName
    }
    echo JsonOutput.prettyPrint(JsonOutput.toJson(value))
}

@NonCPS
Map merge(Map[] sources) {
    if (sources.length == 0) return [:]
    if (sources.length == 1) return sources[0]

    sources.inject([:]) { result, source ->
        if (source && source.containsKey('override') && source['override']) {
            result = source
        }
        else {
            source.each { k, v ->
                if (result[k] instanceof Map && v instanceof Map ) {
                    if (v.containsKey('override') && v['override']) {
                        result[k] = v
                    }
                    else {
                        result[k] = merge(result[k], v)
                    }
                }
                else if (result[k] instanceof List && v instanceof List) {
                    result[k] += v
                    result[k] = result[k].unique()
                }
                else {
                    result[k] = v
                }
            }
        }
        result
    }
}

def removeDir(dir, context) {
    if (fileExists(dir)) {
        drupipeShell("""
            rm -fR ${dir}
            """, context
        )
    }
}

return this
