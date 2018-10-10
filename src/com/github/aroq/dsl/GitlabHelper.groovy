package com.github.aroq.dsl

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7')
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.*

class GitlabHelper {

    def script

    def config

    def setRepoProperties(repo) {
        if (repo == null) {
            script.println "GitlabHelper->setRepoProperties(): Repo is not provided"
        }
        config.repoParams = [:]
        config.repoParams.gitlabAddress = repo.substring(repo.indexOf('@') + 1, repo.indexOf(':'));
        config.repoParams.groupName     = repo.substring(repo.indexOf(':') + 1, repo.indexOf('/'));
        config.repoParams.projectName   = repo.substring(repo.indexOf('/') + 1, repo.lastIndexOf("."));
        config.repoParams.projectID     = "${config.repoParams.groupName}%2F${config.repoParams.projectName}"
    }

    def addWebhook(String repo, url, hookData = [:]) {
        script.println "Add webhooks: ${url}"
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
        data << hookData
        try {
            if (hook_id) {
                data << [hook_id: hook_id]
                http.request("https://${config.repoParams.gitlabAddress}/api/v4/projects/${config.repoParams.projectID}/hooks/${hook_id}", PUT, JSON) {
                    send URLENC, data
                    response.success = { resp, json ->
                        script.println "EDIT HOOK response: ${json}"
                    }
                }
            }
            else {
                http.request("https://${config.repoParams.gitlabAddress}/api/v4/projects/${config.repoParams.projectID}/hooks", POST, JSON) {
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

    def deleteWebhook(String repo, servers, url) {
        setRepoProperties(repo)

        script.println "deleteWebhook Servers: ${servers.toString()}"

        def urls = []
        for (server in servers) {
            urls << server.value.jenkinsUrl.substring(0, server.value.jenkinsUrl.length() - (server.value.jenkinsUrl.endsWith("/") ? 1 : 0)) + '/' + url
        }

        script.println "deleteWebhook URLs: ${urls.toString()}"

        def webhooks = getWebhooks(repo)

        for (webhook in webhooks) {
            if (webhook.url in urls) {
                script.println "SKIP DELETE HOOK IN URLS: ${webhook.toString()}"
            }
            else {
                if (webhook.url.endsWith(url)) {
                    def http = new HTTPBuilder()
                    http.setHeaders([
                        'PRIVATE-TOKEN': config.env.GITLAB_API_TOKEN_TEXT,
                    ])

                    try {
                        if (webhook.id) {
                            script.println "DELETE HOOK: ${config.repoParams.projectID} -> ${webhook.toString()}"
                            http.request("https://${config.repoParams.gitlabAddress}/api/v4/projects/${config.repoParams.projectID}/hooks/${webhook.id}", DELETE, JSON) {
                                response.success = { resp, json ->
                                    script.println "DELETE HOOK response: ${json}"
                                }
                            }
                        }
                    }
                    catch (e) {
                        script.println e
                    }
                }
                else {
                    script.println "SKIP DELETE HOOK FROM ANOTHER JENKINS: ${webhook.toString()}"
                }
            }
        }
    }

    def getBranches(String repo) {
        setRepoProperties(repo)
        def url = "https://${config.repoParams.gitlabAddress}/api/v4/projects/${config.repoParams.projectID}/repository/branches?private_token=${config.env.GITLAB_API_TOKEN_TEXT}"
        def branches = new groovy.json.JsonSlurper().parseText(new URL(url).text)
        branches
    }

    def getBranch(String repo, String branch) {
        setRepoProperties(repo)
        try {
            def url = "https://${config.repoParams.gitlabAddress}/api/v4/projects/${config.repoParams.projectID}/repository/branches/${branch}?private_token=${config.env.GITLAB_API_TOKEN_TEXT}"
            def branch_obj = new groovy.json.JsonSlurper().parseText(new URL(url).text)
            return branch_obj
        }
        catch (Exception e) {
            return false
        }
    }

    def getWebhooks(String repo) {
        script.println "Get webhooks: ${repo}"
        setRepoProperties(repo)
        def url = "https://${config.repoParams.gitlabAddress}/api/v4/projects/${config.repoParams.projectID}/hooks?private_token=${config.env.GITLAB_API_TOKEN_TEXT}"
        def hooks = new groovy.json.JsonSlurper().parseText(new URL(url).text)
        hooks
    }

    def getUsers(String repo) {
        setRepoProperties(repo)
        def users = [:]

        println config
        try {
            def urls = [
                "https://${config.repoParams.gitlabAddress}/api/v4/groups/${config.repoParams.groupName}/members?private_token=${config.env.GITLAB_API_TOKEN_TEXT}",
                "https://${config.repoParams.gitlabAddress}/api/v4/projects/${config.repoParams.projectID}/members?private_token=${config.env.GITLAB_API_TOKEN_TEXT}",
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

    def isGitlabRepo(repo, config) {
        config.env.GITLAB_HOST && repo.contains(config.env.GITLAB_HOST)
    }

}
