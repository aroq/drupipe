import com.github.aroq.dsl.DocmanConfig
import com.github.aroq.dsl.DslHelper
import com.github.aroq.dsl.GitlabHelper

println "Docman Job DSL processing"

def dslHelper = new DslHelper(script: this)
def config = dslHelper.readJson(this, '.unipipe/temp/context_processed.json')
dslHelper.config = config
config.dslHelper = dslHelper

//def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

println "Config tags: ${config.tags}"

if (!config.tags || (!config.tags.contains('docman') && !config.tags.contains('drupipe'))) {
    docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : "${config.docmanDir}/config/config.json"

    def docrootConfigJson
    try {
      docrootConfigJson = readFileFromWorkspace(filePath)
    }
    catch(e) {
        docrootConfigJson = null
    }

    if (config.configSeedType == 'docman' && docrootConfigJson) {
        // Retrieve Docman config from json file (prepared by "docman info" command).
        def docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)
        println "Docman config: ${docmanConfig.init()}"

        if (config.env.GITLAB_API_TOKEN_TEXT) {
            if (config.jenkinsServers.size() == 0) {
                println "Servers empty. Check configuration file servers.(yaml|yml)."
            }

            println 'Servers: ' + config.jenkinsServers.keySet().join(', ')

            println "Initialize Gitlab Helper"
            gitlabHelper = new GitlabHelper(script: this, config: config)
        }

        def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipeline'

// TODO: Use docman config to retrieve info.
        branches = [
            development: [
                pipeline: pipelineScript,
                quietPeriodSeconds: 5,
            ],
            staging: [
                pipeline: pipelineScript,
                quietPeriodSeconds: 5,
            ],
            stable: [
                pipeline: pipelineScript,
                quietPeriodSeconds: 5,
            ],
        ]

        if (config.branches) {
            branches = merge(branches, config.branches)
        }

// Create pipeline jobs for each state defined in Docman config.
        docmanConfig.states?.each { state ->
            def params = config
            println "Processing state: ${state.key}"
            if (branches[state.key]) {
                params = merge(config, branches[state.key])
            }
            if (branches[state.key]?.branch) {
                branch = branches[state.key]?.branch
            }
            else {
                branch = docmanConfig.getVersionBranch('', state.key)
            }
            buildEnvironment = docmanConfig.getEnvironmentByState(state.key)
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
                def webhook_tags
                if (config.params.webhooksEnvironments) {
                    webhook_tags = config.params.webhooksEnvironments
                }
                else if (config.webhooksEnvironments) {
                    webhook_tags = config.webhooksEnvironments
                }
                if (webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                    triggers {
                        if (config.env.GITLAB_API_TOKEN_TEXT) {
                            gitlabPush {
                                buildOnPushEvents()
                                buildOnMergeRequestEvents(false)
                                enableCiSkip()
                                useCiFeatures()
                                includeBranches(branch)
                            }
                        }
                    }
                }
                properties {
                    if (config.env.GITLAB_API_TOKEN_TEXT) {
                        gitLabConnectionProperty {
                            gitLabConnection('Gitlab')
                        }
                    }
                }
            }
            if (config.env.GITLAB_API_TOKEN_TEXT) {
                docmanConfig.projects?.each { project ->
                    if (project.value.type != 'root' && project.value.repo && isGitlabRepo(project.value.repo, config)) {
                        def webhook_tags
                        if (config.params.webhooksEnvironments) {
                            webhook_tags = config.params.webhooksEnvironments
                        }
                        else if (config.webhooksEnvironments) {
                            webhook_tags = config.webhooksEnvironments
                        }
                        println "Webhook Tags: ${webhook_tags}"
                        if (webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                            def tag_servers = getServersByTags(webhook_tags, config.jenkinsServers)
                            gitlabHelper.deleteWebhook(
                                project.value.repo,
                                tag_servers,
                                "project/${config.jenkinsFolderName}/${state.key}"
                            )
                            for (jenkinsServer in tag_servers) {
                                gitlabHelper.addWebhook(
                                    project.value.repo,
                                    jenkinsServer.value.jenkinsUrl.substring(0, jenkinsServer.value.jenkinsUrl.length() - (jenkinsServer.value.jenkinsUrl.endsWith("/") ? 1 : 0)) + '/' + "project/${config.jenkinsFolderName}/${state.key}"
                                )
                            }
                        }
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

def sourcePath(params, sourceName, String path) {
    if (sourceName in params.loadedSources) {
        println "sourcePath: " + params.loadedSources[sourceName].path + '/' + path
        params.loadedSources[sourceName].path + '/' + path
    }
}

def sourceDir(params, sourceName) {
    if (sourceName in params.loadedSources) {
        println "sourceDir: " + params.loadedSources[sourceName].path
        params.loadedSources[sourceName].path
    }
}

def getServersByTags(tags, servers) {
    def result = [:]
    if (tags && tags instanceof ArrayList) {
        for (def i = 0; i < tags.size(); i++) {
            def tag = tags[i]
            for (server in servers) {
                if (server.value?.tags && tag in server.value?.tags && server.value?.jenkinsUrl) {
                    result << ["${server.key}": server.value]
                }
            }
        }
    }
    println "getServersByTags: ${result}"
    result
}

