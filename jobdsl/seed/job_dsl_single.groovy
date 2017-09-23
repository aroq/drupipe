println "Docman Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

if (!config.tags || (!config.tags.contains('drupipe') && config.configSeedType == 'single')) {
    if (config.env.GITLAB_API_TOKEN_TEXT && !config.noHooks) {
        println "Initialize Gitlab Helper"
        config.gitlabHelper = new GitlabHelper(script: this, config: config)
    }

    def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipeline'

// TODO: Use docman config to retrieve info.
    branches = [
        development: [
            pipeline: pipelineScript,
            quietPeriodSeconds: 5,
            environment: 'dev',
            branch: 'develop',
        ],
        staging: [
            pipeline: pipelineScript,
            quietPeriodSeconds: 5,
            environment: 'stage',
            branch: 'staging',
        ],
        stable: [
            pipeline: pipelineScript,
            quietPeriodSeconds: 5,
            environment: 'prod',
            branch: 'state_stable',
        ],
    ]

    if (config.branches) {
        branches = merge(branches, config.branches)
    }

// Create pipeline jobs for each state defined in Docman config.
    config.states?.each { state ->
        def params = config
        println "Processing state: ${state.key}"
        if (branches[state.key]) {
            params = merge(config, branches[state.key])
        }
        if (branches[state.key]?.branch) {
            branch = branches[state.key]?.branch
        }
        buildEnvironment = branches[state.key].environment
        pipelineJob(state.key) {
            if (params.quietPeriodSeconds) {
                quietPeriod(params.quietPeriodSeconds)
            }
            concurrentBuild(false)
            logRotator(-1, config.logRotatorNumToKeep)
            parameters {
                stringParam('projectName', '')
                stringParam('debugEnabled', '0')
                stringParam('force', '0')
                stringParam('simulate', '0')
                stringParam('docrootDir', 'docroot')
                stringParam('config_repo', params.configRepo)
                stringParam('type', 'branch')
                stringParam('environment', buildEnvironment)
                stringParam('version', branch)
            }
            definition {
                cpsScm {
                    scm {
                        git() {
                            remote {
                                name('origin')
                                url(params.configRepo)
                                credentials(params.credentialsId)
                            }
                            extensions {
                                relativeTargetDirectory(config.projectConfigPath)
                            }
                        }
                        scriptPath("${config.projectConfigPath}/pipelines/${params.pipeline}.groovy")
                    }
                }
            }
            triggers {
                gitlabPush {
                    buildOnPushEvents()
                    buildOnMergeRequestEvents(false)
                    enableCiSkip()
                    useCiFeatures()
                    includeBranches(branch)
                }
            }
            properties {
                gitLabConnectionProperty {
                    gitLabConnection('Gitlab')
                }
            }
        }
        if (config.gitlabHelper) {
            config.components.each { project ->
                if (project.value.type != 'root' && project.value.repo && isGitlabRepo(project.value.repo, config)) {
                    if (config.params.webhooksEnvironments.contains(config.env.drupipeEnvironment)) {
                        gitlabHelper.addWebhook(
                            project.value.repo,
                            "${config.env.JENKINS_URL}project/${config.jenkinsFolderName}/${state.key}"
                        )
                    }
                }
            }
        }
    }

}

def isGitlabRepo(repo, config) {
    config.env.GITLAB_HOST && repo.contains(config.env.GITLAB_HOST)
}

Map merge(Map[] sources) {
    if (sources.length == 0) return [:]
    if (sources.length == 1) return sources[0]

    sources.inject([:]) { result, source ->
        source.each { k, v ->
            result[k] = result[k] instanceof Map && v instanceof Map ? merge(result[k], v) : v
        }
        result
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
