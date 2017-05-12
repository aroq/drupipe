import com.github.aroq.GitlabHelper
import com.github.aroq.DocmanConfig

println "Subjobs Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

println "Config tags: ${config.tags}"

if (config.tags.contains('docman')) {
    docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : "${config.projectConfigPath}/config.json"
    docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

    // Retrieve Docman config from json file (prepared by "docman info" command).
    config.docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)
}


config.gitlabHelper = new GitlabHelper(script: this, config: config)

if (config.jobs) {
    processJob(config.jobs, '', config)
}

def processJob(jobs, currentFolder, config) {
    def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipeline'
    jobs.each { job ->
        println "Processing job: ${job.name}"
        def currentName = currentFolder ? "${currentFolder}/${job.name}" : job.name
        println "Type: ${job.type}"
        println "Current name: ${currentName}"
        if (job.type == 'folder') {
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
            currentFolder = currentName
        }
        else {
            if (job.pipeline && job.pipeline.repo_type && job.pipeline.repo_type == 'config') {
                repo = config.configRepo
            }
            if (job.type == 'release-deploy') {
                pipelineJob(currentName) {
                    concurrentBuild(false)
                    logRotator(-1, 30)
                    parameters {
                        config.docmanConfig.projects?.each { project ->
                            if ((project.value.type == 'root' || project.value.type == 'root_chain') && project.value.repo && config.env.GITLAB_HOST && project.value.repo.contains(config.env.GITLAB_HOST)) {
                                println "Project: ${project.value.name}"
                                def releaseRepo = project.value.type == 'root' ? project.value.repo : project.value.root_repo
                                activeChoiceParam('release') {
                                    description('Allows user choose from multiple choices')
                                    filterable()
                                    choiceType('SINGLE_SELECT')
                                    scriptlerScript("git_${job.source.type}.groovy") {
                                        parameter('url', releaseRepo)
                                        parameter('tagPattern', ${job.source.pattern})
                                    }
                                }
                                stringParam('environment', ${job.env})
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
                                        relativeTargetDirectory(config.projectConfigPath)
                                    }
                                }
                                scriptPath("${config.projectConfigPath}/pipelines/${pipelineScript}.groovy")
                            }
                        }
                    }
                }

            }
            if (job.type == 'selenese') {
                def repo = config.defaultActionParams.SeleneseTester.repoAddress
                def b = config.defaultActionParams.SeleneseTester.reference ? config.defaultActionParams.SeleneseTester.reference : 'master'

                if (config.env.GITLAB_API_TOKEN_TEXT) {
                    users = config.gitlabHelper.getUsers(repo)
                    println "USERS: ${users}"
                }

                pipelineJob("${currentName}") {
                    concurrentBuild(false)
                    logRotator(-1, 30)
                    parameters {
                        stringParam('debugEnabled', '0')
                        activeChoiceParam('suites') {
                            description('Select one or more suites. If you see the empty list - please re-save the job (related to bug: https://issues.jenkins-ci.org/browse/JENKINS-42655)')
                            filterable()
                            choiceType('MULTI_SELECT')
                            groovyScript {
                                // NOTE: https://issues.jenkins-ci.org/browse/JENKINS-42655?page=com.atlassian.jira.plugin.system.issuetabpanels%3Aall-tabpanel
                                script('["' + job.suites.collect{ it += ':selected' }.join('", "') + '"]')
                            }
                        }
                    }
                    definition {
                        cpsScm {
                            scm {
                                git() {
                                    remote {
                                        name('origin')
                                        url(repo)
                                        credentials(config.credentialsId)
                                    }
                                    extensions {
                                        relativeTargetDirectory(config.projectConfigPath)
                                    }
                                    branch(b)
                                }
                                scriptPath(job.pipeline.file)
                            }
                        }
                    }
                }
            }
        }

        if (job.children) {
            processJob(job.children, currentFolder, config)
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
