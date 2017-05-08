import com.github.aroq.GitlabHelper

println "Subconfigs Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : 'docroot/config/config.json'
docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

if (config.subconfigs) {
    if (config.env.GITLAB_API_TOKEN_TEXT) {
        println "Initialize Gitlab Helper"
        gitlabHelper = new GitlabHelper(script: this, config: config)
    }

    def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipeline'

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
            logRotator(-1, 30)
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
                                relativeTargetDirectory('docroot/config')
                            }
                        }
                        scriptPath("docroot/config/pipelines/${params.pipeline}.groovy")
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
        if (config.env.GITLAB_API_TOKEN_TEXT) {
            config.components.each { project ->
                if (project.value.type != 'root' && project.value.repo && isGitlabRepo(project.value.repo, config)) {
                    if (config.webhooksEnvironments.contains(config.env.drupipeEnvironment)) {
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
