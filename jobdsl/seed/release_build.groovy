import com.github.aroq.DocmanConfig

println "Release build Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : 'docroot/config/config.json'
docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

// Retrieve Docman config from json file (prepared by "docman info" command).
def docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)

if (config.releaseEnvs) {
    pipelineJob("release-build") {
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            docmanConfig.projects?.each { project ->
                if (project.value.repo) {
                    println "Project: ${project.value.name}"
                    def repo = project.value.repo
                    println "Repo: ${repo}"
                    def gitlabAddress = repo.substring(repo.indexOf('@') + 1, repo.indexOf(':'));
                    def groupName     = repo.substring(repo.indexOf(':') + 1, repo.indexOf('/'));
                    def projectName   = repo.substring(repo.indexOf('/') + 1, repo.lastIndexOf("."));
                    def projectID     = "${groupName}%2F${projectName}"
                    def privateToken = "${config.env.GITLAB_API_TOKEN_TEXT}"
                    activeChoiceParam("${project.value.name}_version") {
                        description('Allows user choose from multiple choices')
                        filterable()
                        choiceType('SINGLE_SELECT')
                        scriptlerScript('git_tags.groovy') {
                            parameter('url', repo)
                            parameter('tagPattern', "*")
                        }
                    }
                }
            }
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
                        extensions {
                            relativeTargetDirectory('docroot/config')
                        }
                    }
                    scriptPath("docroot/config/pipelines/pipeline.groovy")
                }
            }
        }
    }
}

Map merge(Map[] sources) {
    if (sources.length == 0) return [:]
    if (sources.length == 1) return sources[0]

    sources.inject([:]) { result, source ->
        source.each { k, v ->
            result[k] = result[k] instanceof Map ? merge(result[k], v) : v
        }
        result
    }
}
