import groovy.json.JsonSlurper
import com.github.aroq.GitlabHelper

def configMain = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))
def projects = JsonSlurper.newInstance().parseText(readFileFromWorkspace('projects.json')).projects

def gitlabHelper = new GitlabHelper(script: this, config: configMain)

projects.each { project ->
    println "PROJECT: ${project.value}"
    def config = configMain.clone()
    config << project.value
    println "CONFIG mothership_job_subDir: ${config.mothership_job_subDir}"
    println "CONFIG mothership_job_name: ${config.mothership_job_name}"
    String subDir = config.mothership_job_subDir ? config.mothership_job_subDir + '/' : ''
    if (config.mothership_job_type == 'Jenkinsfile') {
        String jobName = config.mothership_job_name ? config.mothership_job_name : project.key
        println "JOB NAME: ${jobName}"
        def users = [:]

        // TODO: Add condition checking if permissions should be set based on Gitlab permissions.
        // TODO: Add condition checking if repo is in Gitlab.
        if (config.env.GITLAB_API_TOKEN_TEXT) {
            users = gitlabHelper.getUsers(config.configRepo)
            println "USERS: ${users}"
        }

        folder(project.key) {
            authorization {
                users.each { user ->
                    // TODO: make permissions configurable.
                    if (user.value > 10) {
                        permission('hudson.model.Item.Read', user.key)
                        println "Added READ permissions for user:${user.key}, folder: ${project.key}"
                    }
                    if (user.value > 30) {
                        permission('hudson.model.Run.Update', user.key)
                        permission('hudson.model.Item.Build', user.key)
                        permission('hudson.model.Item.Cancel', user.key)
                        println "Added UPDATE/BUILD/CANCEL permissions for user:${user.key}", folder: ${project.key}
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
                                url(config.configRepo)
                                credentials(config.credentialsId)
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
                config.configRepo,
                "${config.env.JENKINS_URL}project/${project.key}/seed"
            )
        }
    }
    else if (config.mothership_job_type == 'multibranch') {
        multibranchPipelineJob(project.key) {
            branchSources {
                git {
                    remote(config.configRepo)
                    credentialsId(config.credentialsId)
                }
            }
        }
    }
}

