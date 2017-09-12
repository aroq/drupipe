def configMain = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

def projects = []
if (fileExists('projects.yaml')) {
    projects = readYaml(file: 'projects.yaml').projects
}
else if (fileExists('projects.yml')) {
    projects = readYaml(file: 'projects.yml').projects
}
else if (fileExists('projects.json')) {
    projects = JsonSlurper.newInstance().parseText(readFileFromWorkspace('projects.json')).projects
}

def gitlabHelper = new GitlabHelper(script: this, config: configMain)

projects.each { project ->
    println "PROJECT: ${project.value}"
    def config = configMain.clone()
    config << project.value
    println "CONFIG mothership_job_subDir: ${config.mothership_job_subDir}"
    println "CONFIG mothership_job_name: ${config.mothership_job_name}"
    String subDir = config.mothership_job_subDir ? config.mothership_job_subDir + '/' : ''
    if (config.mothership_job_type == 'Jenkinsfile') {
        String jobName = config.mothership_job_name ? config.mothership_job_name : project.key
        println "JOB NAME: ${jobName}"
        def users = [:]

        // TODO: Add condition checking if permissions should be set based on Gitlab permissions.
        // TODO: Add condition checking if repo is in Gitlab.
        if (config.env.GITLAB_API_TOKEN_TEXT && !config.noHooks) {
            users = gitlabHelper.getUsers(config.configRepo)
            println "USERS: ${users}"
        }

        println "FOLDER: ${project.key}"
        folder(project.key) {
            authorization {
                users.each { user ->
                    // TODO: make permissions configurable.
                    if (user.value > 10) {
                        permission('hudson.model.Item.Read', user.key)
                        println "Added READ permissions for user:${user.key}, folder: ${project.key}"
                    }
                    if (user.value > 30) {
                        permission('hudson.model.Run.Update', user.key)
                        permission('hudson.model.Item.Build', user.key)
                        permission('hudson.model.Item.Cancel', user.key)
                        println "Added UPDATE/BUILD/CANCEL permissions for user:${user.key}, folder: ${project.key}"
                    }
                }
            }
        }

        pipelineJob("${project.key}/${jobName}") {
            concurrentBuild(false)
            logRotator(-1, config.logRotatorNumToKeep)
            parameters {
                stringParam('debugEnabled', '0')
                stringParam('force', '0')
            }
            definition {
                cpsScm {
                    scm {
                        git() {
                            remote {
                                name('origin')
                                url(config.configRepo)
                                credentials(config.credentialsId)
                            }
                            branch('master')
                            extensions {
                                relativeTargetDirectory(subDir)
                            }
                        }
                        scriptPath("${subDir}Jenkinsfile")
                    }
                }
            }
            triggers {
                gitlabPush {
                    buildOnPushEvents()
                    buildOnMergeRequestEvents(false)
                    enableCiSkip()
                    useCiFeatures()
                    includeBranches('master')
                }
            }
            properties {
                gitLabConnectionProperty {
                    gitLabConnection('Gitlab')
                }
            }
        }
        if (config.env.GITLAB_API_TOKEN_TEXT && !config.noHooks) {
            gitlabHelper.addWebhook(
                config.configRepo,
                "${config.env.JENKINS_URL}project/${project.key}/seed"
            )
        }
    }
    else if (config.mothership_job_type == 'multibranch') {
        multibranchPipelineJob(project.key) {
            branchSources {
                git {
                    remote(config.configRepo)
                    credentialsId(config.credentialsId)
                }
            }
        }
    }
}

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7')
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.*

class GitlabHelper {

    def script

    def config

    def setRepoProperties(repo) {
        config.repoParams.gitlabAddress = repo.substring(repo.indexOf('@') + 1, repo.indexOf(':'));
        config.repoParams.groupName     = repo.substring(repo.indexOf(':') + 1, repo.indexOf('/'));
        config.repoParams.projectName   = repo.substring(repo.indexOf('/') + 1, repo.lastIndexOf("."));
        config.repoParams.projectID     = "${config.repoParams.groupName}%2F${config.repoParams.projectName}"
    }

    def addWebhook(String repo, url) {
        setRepoProperties(repo)
        def hook_id = null
        getWebhooks(repo).each { hook ->
            if (hook.url.contains(url)) {
                script.println "FOUND HOOK: ${hook.url}"
                hook_id = hook.id
            }
        }
        def http = new HTTPBuilder()
        http.setHeaders([
            'PRIVATE-TOKEN': config.env.GITLAB_API_TOKEN_TEXT,
        ])
        def data = [id: "${config.repoParams.groupName}/${config.repoParams.projectName}", url: url, push_events: true]
        try {
            if (hook_id) {
                data << [hook_id: hook_id]
                http.request("https://${config.repoParams.gitlabAddress}/api/v3/projects/${config.repoParams.projectID}/hooks/${hook_id}", PUT, JSON) {
                    send URLENC, data
                    response.success = { resp, json ->
                        script.println "EDIT HOOK response: ${json}"
                    }
                }
            }
            else {
                http.request("https://${config.repoParams.gitlabAddress}/api/v3/projects/${config.repoParams.projectID}/hooks", POST, JSON) {
                    send URLENC, data
                    response.success = { resp, json ->
                        script.println "ADD HOOK response: ${json}"
                    }
                }
            }
        }
        catch (e) {
            script.println e
        }
    }

    def getWebhooks(String repo) {
        setRepoProperties(repo)
        def url = "https://${config.repoParams.gitlabAddress}/api/v3/projects/${config.repoParams.projectID}/hooks?private_token=${config.env.GITLAB_API_TOKEN_TEXT}"
        def hooks = new groovy.json.JsonSlurper().parseText(new URL(url).text)
        hooks
    }

    def getUsers(String repo) {
        setRepoProperties(repo)
        def users = [:]

        println config
        try {
            def urls = [
                "https://${config.repoParams.gitlabAddress}/api/v3/groups/${config.repoParams.groupName}/members?private_token=${config.env.GITLAB_API_TOKEN_TEXT}",
                "https://${config.repoParams.gitlabAddress}/api/v3/projects/${config.repoParams.projectID}/members?private_token=${config.env.GITLAB_API_TOKEN_TEXT}",
            ]
            urls.each { url ->
                def gitlabUsers = new groovy.json.JsonSlurper().parseText(new URL(url).text)
                users << gitlabUsers.collectEntries { user ->
                    [(user.username): user.access_level]
                }
            }
        }
        catch (e) {
            println e
        }
        script.println users
        users
    }
}

import groovy.json.JsonSlurper

/**
 * Created by Aroq on 06/06/16.
 */
class DocmanConfig {

    def docrootConfigJson

    def docmanConfig

    def script

    def init() {
        docmanConfig = JsonSlurper.newInstance().parseText(docrootConfigJson)
    }

    def getProjects() {
        init()
        docmanConfig['projects']
    }

    def getStates() {
        init()
        docmanConfig['states']
    }

    def getEnvironmentByState(String stateName) {
        def states = getStates()
        states[stateName]
    }

    def getEnvironments() {
        init()
        docmanConfig['environments']
    }

    def getVersionBranch(project, stateName) {
        init()
        if (!project) {
            // TODO: retrieve first project from docman config.
            def projectMap = docmanConfig.projects.find { it.value.containsKey('states') }
            if (projectMap) {
                project = projectMap.key
                script.println "First project name: ${project}"
            }
            else {
                throw new RuntimeException("Project with states is not found in ${docmanConfig.projects}")
            }

        }
        if (docmanConfig.projects[project]['states'][stateName]) {
            if (docmanConfig.projects[project]['states'][stateName]['version']) {
                docmanConfig.projects[project]['states'][stateName]['version']
            }
            else if (docmanConfig.projects[project]['states'][stateName]['source']) {
                docmanConfig.projects[project]['states'][stateName]['source']['branch']
            }
        }
        else {
            throw new RuntimeException("There is no state ${stateName} defined in project ${docmanConfig.projects[project]}")
        }
    }

}
