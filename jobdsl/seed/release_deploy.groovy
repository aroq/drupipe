import com.github.aroq.DocmanConfig

println "Release build Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : 'docroot/config/config.json'
docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

// Retrieve Docman config from json file (prepared by "docman info" command).
def docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)

if (config.releaseEnvs) {
    config.releaseEnvs.each { e ->
        pipelineJob("release-deploy-${e.name}") {
            concurrentBuild(false)
            logRotator(-1, 30)
            parameters {
                docmanConfig.projects?.each { project ->
                    if ((project.value.type == 'root' || project.value.type == 'root_chain') && project.value.repo && config.env.GITLAB_HOST && project.value.repo.contains(config.env.GITLAB_HOST)) {
                        println "Project: ${project.value.name}"
                        def repo = project.value.type == 'root' ? project.value.repo : project.value.root_repo
                        activeChoiceParam('release') {
                            description('Allows user choose from multiple choices')
                            filterable()
                            choiceType('SINGLE_SELECT')
                            scriptlerScript("git_${e.type}.groovy") {
                                parameter('url', repo)
                                parameter('tagPattern', e.pattern)
                            }
                        }
                        stringParam('environment', e.env)
                        stringParam('debugEnabled', '0')
                        stringParam('force', '0')
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
