import groovy.json.JsonSlurper

projects = JsonSlurper.newInstance().parseText(readFileFromWorkspace('projects.json')).projects

projects.each {project ->
    def subDir = project.value['subDir'] ? project.value['subDir'] : ''

    if (project.value['type'] == 'Jenkinsfile') {
        pipelineJob("seed") {
            concurrentBuild(false)
            logRotator(-1, 30)
            parameters {
                stringParam('debug', '0')
                stringParam('force', '0')
            }
            definition {
                cpsScm {
                    scm {
                        git() {
                            remote {
                                name('origin')
                                url(project.value['repo'])
                            }
                            extensions {
                                relativeTargetDirectory(subDir)
                            }
                        }
                        scriptPath('Jenkinsfile')
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
    }
}

