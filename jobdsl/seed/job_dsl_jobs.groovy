import com.github.aroq.GitlabHelper

println "Subjobs Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

def gitlabHelper = new GitlabHelper(script: this, config: config)

if (config.jobs) {
    def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipeline'

    def currentName = ''

    def repo = config.defaultActionParams.SeleneseTester.repoAddress
    def branch = config.defaultActionParams.SeleneseTester.reference

    if (config.env.GITLAB_API_TOKEN_TEXT) {
        users = gitlabHelper.getUsers(repo)
        println "USERS: ${users}"
    }

    processJob(config.jobs, currentName, users, repo, branch)

}

def processJob(jobs, currentName, users, repo, branch) {
    jobs.each { j ->
        println "Processing job: ${j.name}"
        if (j.type == 'folder') {
            currentName = currentName ? "${currentName}/${j.name}" : j.name
            folder(currentName) {
                authorization {
                    users.each { user ->
                        // TODO: make permissions configurable.
                        if (user.value > 10) {
                            permission('hudson.model.Item.Read', user.key)
                        }
                        if (user.value > 30) {
                            permission('hudson.model.Run.Update', user.key)
                            permission('hudson.model.Item.Build', user.key)
                            permission('hudson.model.Item.Cancel', user.key)
                        }
                    }
                }
            }
        }
        else if (job.type == 'selenese') {
            pipelineJob("${currentName}") {
                concurrentBuild(false)
                logRotator(-1, 30)
                parameters {
                    stringParam('debugEnabled', '0')
                }
                definition {
                    cpsScm {
                        scm {
                            git() {
                                remote {
                                    name('origin')
                                    url(config.repo)
                                    credentials(config.credentialsId)
                                }
                                branch(branch)
                            }
                            scriptPath("Jenkinsfile")
                        }
                    }
                }
            }
        }

        if (j.children) {
            processJob(j.children, currentFolder, users, repo, branch)
        }
    }
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
