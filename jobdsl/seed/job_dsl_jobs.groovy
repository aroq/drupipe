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

if (config.env.GITLAB_API_TOKEN_TEXT) {
    config.gitlabHelper = new GitlabHelper(script: this, config: config)
}


if (config.jobs) {
    processJob(config.jobs, '', config)
}

def processJob(jobs, currentFolder, config) {
    def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipelines/pipeline'
    for (job in jobs) {
        println job
        println "Processing job: ${job.key}"
        def currentName = currentFolder ? "${currentFolder}/${job.key}" : job.key
        println "Type: ${job.value.type}"
        println "Current name: ${currentName}"
        if (job.value.type == 'folder') {
            folder(currentName) {
                if (config.gitlabHelper) {
                    users = config.gitlabHelper.getUsers(config.configRepo)
                    println "USERS: ${users}"
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
//            currentFolder = currentName
        }
        else {
            if (job.value.pipeline && job.value.pipeline.repo_type && job.value.pipeline.repo_type == 'config') {
                repo = config.configRepo
            }
            if (job.value.type == 'release-build') {
                pipelineJob(currentName) {
                    concurrentBuild(false)
                    logRotator(-1, 30)
                    parameters {
                        config.docmanConfig.projects?.each { project ->
                            if (project.value.repo && (project.value.type != 'root')) {
                                println "Project: ${project.value.name}"
                                def projectRepo = project.value.repo
                                println "Repo: ${projectRepo}"
                                activeChoiceParam("${project.value.name}_version") {
                                    description('Allows user choose from multiple choices')
                                    filterable()
                                    choiceType('SINGLE_SELECT')
                                    scriptlerScript('git_tags.groovy') {
                                        parameter('url', projectRepo)
                                        parameter('tagPattern', "*")
                                        parameter('sort', 'x.y.z')
                                    }
                                }
                            }
                        }
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
                                    extensions {
                                        relativeTargetDirectory(config.projectConfigPath)
                                    }
                                }
                                scriptPath("${config.projectConfigPath}/${pipelineScript}.groovy")
                            }
                        }
                    }
                }

            }
            else if (job.value.type == 'state') {
                def state = job.value.state
                if (config.docmanConfig) {
                    buildEnvironment = config.docmanConfig.getEnvironmentByState(state)
                    branch = config.docmanConfig.getVersionBranch('', state)
                }
                else {
                    // TODO: Check it.
                    buildEnvironment = job.value.env
                    branch = job.value.branch
                }
                pipelineJob(currentName) {
                    if (config.quietPeriodSeconds) {
                        quietPeriod(config.quietPeriodSeconds)
                    }
                    concurrentBuild(false)
                    logRotator(-1, 30)
                    parameters {
                        stringParam('projectName', 'master')
                        stringParam('debugEnabled', '0')
                        stringParam('force', '0')
                        stringParam('simulate', '0')
                        stringParam('docrootDir', 'docroot')
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
                                        url(config.configRepo)
                                        credentials(config.credentialsId)
                                    }
                                    extensions {
                                        relativeTargetDirectory(config.projectConfigPath)
                                    }
                                }
                                scriptPath("${config.projectConfigPath}/${pipelineScript}.groovy")
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
                if (config.docmanConfig) {
                    if (config.env.GITLAB_API_TOKEN_TEXT) {
                        println "Processing Gitlab webhooks"
                        config.docmanConfig.projects?.each { project ->
                            println "Project: ${project}"
                            if (project.value.type != 'root' && project.value.repo && isGitlabRepo(project.value.repo, config)) {
                                if (config.webhooksEnvironments.contains(config.env.drupipeEnvironment)) {
                                    config.gitlabHelper.addWebhook(
                                        project.value.repo,
                                        "${config.env.JENKINS_URL}project/${config.jenkinsFolderName}/${currentName}"
                                    )
                                    println "Webhook added for project ${project}"
                                }
                            }
                        }
                    }
                }
            }
            else if (job.value.type == 'release-deploy') {
                pipelineJob(currentName) {
                    concurrentBuild(false)
                    logRotator(-1, 30)
                    parameters {
                        config.docmanConfig.projects?.each { project ->
                            if ((project.value.type == 'root' || project.value.type == 'root_chain') && project.value.repo) {
                                println "Project: ${project.value.name}"
                                def releaseRepo = project.value.type == 'root' ? project.value.repo : project.value.root_repo
                                activeChoiceParam('release') {
                                    description('Allows user choose from multiple choices')
                                    filterable()
                                    choiceType('SINGLE_SELECT')
                                    scriptlerScript("git_${job.value.source.type}.groovy") {
                                        parameter('url', releaseRepo)
                                        parameter('tagPattern', job.value.source.pattern)
                                        parameter('sort', '')
                                    }
                                }
                                if (config.operationsModes) {
                                    activeChoiceParam('operationsMode') {
                                        description('Choose the mode for the operations')
                                        groovyScript {
                                            // NOTE: https://issues.jenkins-ci.org/browse/JENKINS-42655?page=com.atlassian.jira.plugin.system.issuetabpanels%3Aall-tabpanel
                                            script('["' + config.operationsModes.join('", "') + '"]')
                                        }
                                    }
                                }
                                stringParam('environment', job.value.env)
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
                                scriptPath("${config.projectConfigPath}/${pipelineScript}.groovy")
                            }
                        }
                    }
                }
            }
            else if (job.value.type == 'common') {
                pipelineJob("${currentName}") {
                    concurrentBuild(false)
                    logRotator(-1, 30)
                    parameters {
                        stringParam('debugEnabled', '0')
                        stringParam('configRepo', repo)
                        job.value.params?.each { key, value ->
                            stringParam(key, value)
                        }
                    }
                    definition {
                        cpsScm {
                            scm {
                                git() {
                                    remote {
                                        name('origin')
                                        url(config.configrepo)
                                        credentials(config.credentialsId)
                                    }
                                    extensions {
                                        relativeTargetDirectory(config.projectConfigPath)
                                    }
                                    branch('master')
                                }
                                scriptPath("${config.projectConfigPath}/${pipelineScript}.groovy")
                            }
                        }
                    }
                }

            }
            else if (job.value.type == 'selenese') {
//                def repo = config.defaultActionParams.SeleneseTester.repoAddress
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
                        stringParam('configRepo', repo)
                        activeChoiceParam('suites') {
                            description('Select one or more suites. If you see the empty list - please re-save the job (related to bug: https://issues.jenkins-ci.org/browse/JENKINS-42655)')
                            filterable()
                            choiceType('MULTI_SELECT')
                            groovyScript {
                                // NOTE: https://issues.jenkins-ci.org/browse/JENKINS-42655?page=com.atlassian.jira.plugin.system.issuetabpanels%3Aall-tabpanel
                                script('["' + job.value.suites.collect{ it += ':selected' }.join('", "') + '"]')
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
                                scriptPath("${config.projectConfigPath}/${pipelineScript}.groovy")
                            }
                        }
                    }
                }
            }
        }

        if (job.value.children) {
            processJob(job.value.children, currentName, config)
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

def isGitlabRepo(repo, config) {
    config.env.GITLAB_HOST && repo.contains(config.env.GITLAB_HOST)
}
