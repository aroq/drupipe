import groovy.json.JsonSlurper
import com.github.aroq.GitlabHelper

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))
def projects = JsonSlurper.newInstance().parseText(readFileFromWorkspace('projects.json')).projects

def gitlabHelper = new GitlabHelper(script: this, config: config)

projects.each { project ->
    String subDir = project.value['subDir'] ? project.value['subDir'] + '/' : ''
    if (project.value['type'] == 'Jenkinsfile') {
        String jobName = project.value['name'] ? project.value['name'] : project.key
        def users = [:]

        // TODO: Add condition checking if permissions should be set based on Gitlab permissions.
        // TODO: Add condition checking if repo is in Gitlab.
        if (config.env.GITLAB_API_TOKEN_TEXT) {
            users = gitlabHelper.getUsers(project.value['repo'])
            println "USERS: ${users}"
        }

        folder(project.key) {
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

        pipelineJob("${project.key}/${jobName}") {
            concurrentBuild(false)
            logRotator(-1, 30)
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
        if (config.env.GITLAB_API_TOKEN_TEXT) {
            gitlabHelper.addWebhook(
                project.value.repo,
                "${config.env.JENKINS_URL}project/${project.key}/seed"
            )
        }
    }
    else if (project.value['type'] == 'multibranch') {
        multibranchPipelineJob(project.key) {
            branchSources {
                git {
                    remote(project.value['repo'])
                    credentialsId(project.value['credentialsId'])
                }
            }
        }
    }
}

