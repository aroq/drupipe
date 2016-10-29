import groovy.json.JsonSlurper

projects = JsonSlurper.newInstance().parseText(readFileFromWorkspace('projects.json')).projects

projects.each {project ->
    def subDir = project.value['subDir'] ? project.value['subDir'] + '/' : ''
    if (project.value['type'] == 'Jenkinsfile') {
        def jobName = project.value['name'] ? project.value['name'] : project.key
        folder(project.key)
        pipelineJob("${project.key}/${jobName}") {
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
                                credentials(project.value['credentialsId'])
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
    }
    else if (project.value['type'] == 'multibranch') {
        multibranchPipelineJob(project.key) {
            branchSources {
                git {
                    remote(project.value['repo'])
                }
            }
        }
    }
}

