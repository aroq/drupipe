import com.github.aroq.dsl.DocmanConfig
import com.github.aroq.dsl.GitlabHelper
import com.github.aroq.dsl.DslHelper
import com.github.aroq.dsl.DslParamsHelper

println "Subjobs Job DSL processing"

//def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

def dslHelper = new DslHelper(script: this)
def config = dslHelper.readJson(this, '.unipipe/temp/context_processed.json')
dslHelper.config = config
config.dslHelper = dslHelper

println "Config tags: ${config.tags}"

if (config.tags && config.tags.contains('docman')) {
    docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : "${config.docmanDir}/config/config.json"
    docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

    // Retrieve Docman config from json file (prepared by "docman info" command).
    config.docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)
}

if (config.env.GITLAB_API_TOKEN_TEXT && !config.noHooks) {
    if (config.jenkinsServers.size() == 0) {
        println "Servers empty. Check configuration file servers.(yaml|yml)."
    }

    println 'Servers: ' + config.jenkinsServers.keySet().join(', ')

    config.gitlabHelper = new GitlabHelper(script: this, config: config)
}

//config.dslHelper = new DslHelper(script: this, config: config)
config.dslParamsHelper = new DslParamsHelper(script: this, config: config)

if (config.jobs) {
    processJob(config.jobs, '', config)
}

def processJob(jobs, currentFolder, config) {
    def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipelines/pipeline'

    for (job in jobs) {
        if (job.key == 'seed' || !job.value) {
            continue
        }

//        def parentConfigParams = [:]
//        parentConfigParams << parentConfigParamsPassed
        println job
        println "Processing job: ${job.key}"
        def currentName = currentFolder ? "${currentFolder}/${job.key}" : job.key
        println "Type: ${job.value.type}"
        println "Current name: ${currentName}"

        println "Job: ${job.value}"
        job.value.params = job.value.params ? job.value.params : [:]
//        job.value.params << (parentConfigParams << job.value.params)
//        println "Job params after parent params merge: ${job.value.params}"

        if (job.value.type == 'folder') {
//            parentConfigParams << job.value.params
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
                def seedRepo = config.configRepo
                def localConfig = config.clone()
                if (job.value.context) {
                    localConfig = config.dslHelper.merge(localConfig, job.value.context)
                }
                String pipelineScriptName = config.dslHelper.getPipelineScriptName()
                String pipelinesRepo = config.dslHelper.getPipelineRepo(localConfig, job)
                String pipelineScriptDirPath = config.dslHelper.getPipelineScriptDirPath(localConfig, job)
                String pipelineScriptDirPathPrefix = (pipelineScriptDirPath && pipelineScriptDirPath.length() > 0) ? "${pipelineScriptDirPath}/" : ""
                String pipelineScriptPath = (config.config_dir && config.config_dir.length() > 0) ? "${pipelineScriptDirPathPrefix}${config.config_dir}/${pipelineScriptName}" : "${pipelineScriptDirPathPrefix}${pipelineScriptName}"
                if (config.pipeline_script_full) {
                    pipelineScriptPath = "${config.pipeline_script_full}"
                }
                println "pipelinesRepo: ${pipelinesRepo}"
                println "pipelineScriptPath: ${pipelineScriptPath}"
                println "pipelineScriptDirPath: ${pipelineScriptDirPath}"
                pipelineJob(currentName) {
                    concurrentBuild(false)
                    logRotator(-1, config.logRotatorNumToKeep)
                    parameters {
                        config.docmanConfig.projects?.each { project ->
                            if (project.value.repo && project.value.type != 'root') {
                                println "Project type: ${project.value.type}"
                                println "Project repo: ${project.value.repo}"
                                config.dslParamsHelper.drupipeParamTagsSelectsRelease(delegate, job, config, project.value.name + '_version', project)
                            }
                        }
                        config.dslParamsHelper.drupipeParamsDefault(delegate, job, config)
                    }
                    definition {
                        cpsScm {
                            scm {
                                git() {
                                    branch('master')
                                    remote {
                                        name('origin')
                                        url(pipelinesRepo)
                                        credentials(config.credentialsId)
                                    }
                                    if (pipelineScriptDirPath && pipelineScriptDirPath.length() > 0) {
                                        extensions {
                                            relativeTargetDirectory(pipelineScriptDirPath)
                                        }
                                    }
                                }
                                scriptPath(pipelineScriptPath)
                            }
                        }
                    }
                }

            }
            else if (job.value.type == 'state') {
                def seedRepo = config.configRepo
                def localConfig = config.clone()
                if (job.value.context) {
                    localConfig = config.dslHelper.merge(localConfig, job.value.context)
                }
//                println "Local config: ${localConfig}"
                def state = job.value.state
                def buildEnvironment
                def jobBranch
                if (config.docmanConfig && config.docmanConfig.getType() != 'root') {
                    buildEnvironment = config.docmanConfig.getEnvironmentByState(state)
                    jobBranch = config.docmanConfig.getVersionBranch('', state)
                }
                else if (config.tags && config.tags.contains('single') && config.components && config.states && config.states.containsKey(state) && config.components.containsKey('master') && config.components.master.containsKey('states') && config.components.master.states.containsKey(state) && config.components.master.states[state].containsKey('version')) {
                    buildEnvironment = config.states[state]
                    jobBranch = config.components.master.states[state].version
                }
                else {
                    // TODO: Check it.
                    buildEnvironment = job.value.env
                    jobBranch = job.value.branch
                }
                String pipelineScriptName = config.dslHelper.getPipelineScriptName()
                String pipelinesRepo = config.dslHelper.getPipelineRepo(localConfig, job)
                String pipelineScriptDirPath = config.dslHelper.getPipelineScriptDirPath(localConfig, job)
                String pipelineScriptDirPathPrefix = (pipelineScriptDirPath && pipelineScriptDirPath.length() > 0) ? "${pipelineScriptDirPath}/" : ""
                String pipelineScriptPath = (config.config_dir && config.config_dir.length() > 0) ? "${pipelineScriptDirPathPrefix}${config.config_dir}/${pipelineScriptName}" : "${pipelineScriptDirPathPrefix}${pipelineScriptName}"
                if (config.pipeline_script_full) {
                    pipelineScriptPath = "${config.pipeline_script_full}"
                }
                println "pipelinesRepo: ${pipelinesRepo}"
                println "pipelineScriptPath: ${pipelineScriptPath}"
                println "pipelineScriptDirPath: ${pipelineScriptDirPath}"
                pipelineJob(currentName) {
                    if (config.quietPeriodSeconds) {
                        quietPeriod(config.quietPeriodSeconds)
                    }
                    concurrentBuild(false)
                    logRotator(-1, config.logRotatorNumToKeep)
                    parameters {
                        stringParam('projectName', 'master')
                        stringParam('simulate', '0')
                        stringParam('docrootDir', 'docroot')
                        stringParam('type', 'branch')
                        stringParam('environment', buildEnvironment)
                        stringParam('version', jobBranch)
                        config.dslParamsHelper.drupipeParamsDefault(delegate, job, config)
                    }
                    definition {
                        cpsScm {
                            scm {
                                git {
                                    def selectedRemoteBranch = 'master'
                                    if (config.containsKey('tags') && config.tags.contains('single') && jobBranch != 'state_stable') {
                                        def gitLabBranchResponse = config.gitlabHelper.getBranch(config.configRepo, jobBranch)
                                        if (gitLabBranchResponse && gitLabBranchResponse.containsKey('name')) {
                                            selectedRemoteBranch = gitLabBranchResponse.name
                                        }
                                    }
                                    branch(selectedRemoteBranch)
                                    remote {
                                        name('origin')
                                        url(pipelinesRepo)
                                        credentials(config.credentialsId)
                                    }
                                    if (pipelineScriptDirPath && pipelineScriptDirPath.length() > 0) {
                                        extensions {
                                            relativeTargetDirectory(pipelineScriptDirPath)
                                        }
                                    }
                                }
                                scriptPath(pipelineScriptPath)
                            }
                        }
                    }
                    if (job.value.containsKey('webhook_trigger') && (job.value.webhook_trigger == 0 || job.value.webhook_trigger == '0' || job.value.webhook_trigger == false || job.value.webhook_trigger == 'false')) {
                        println "Triggers disabled by webhook_trigger configuration option"
                    }
                    else {
                        def webhook_tags
                        if (config.params.webhooksEnvironments) {
                            webhook_tags = config.params.webhooksEnvironments
                        }
                        else if (config.webhooksEnvironments) {
                            webhook_tags = config.webhooksEnvironments
                        }
                        if (webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                            triggers {
                                if (config.env.GITLAB_API_TOKEN_TEXT) {
                                    gitlabPush {
                                        buildOnPushEvents()
                                        buildOnMergeRequestEvents(false)
                                        enableCiSkip()
                                        useCiFeatures()
                                        includeBranches(jobBranch)
                                    }
                                }
                            }
                        }
                    }
                    properties {
                        if (config.env.GITLAB_API_TOKEN_TEXT) {
                            gitLabConnectionProperty {
                                gitLabConnection('Gitlab')
                            }
                        }
                    }
                }
                if (config.docmanConfig) {
                    if (config.env.GITLAB_API_TOKEN_TEXT) {
                        if (job.value.containsKey('webhook_trigger') && (job.value.webhook_trigger == 0 || job.value.webhook_trigger == '0' || job.value.webhook_trigger == false || job.value.webhook_trigger == 'false')) {
                            println "Webhooks creation disabled by webhook_trigger configuration option"
                        }
                        else {
                            println "Processing Gitlab webhooks for Docman project type"
                            config.docmanConfig.projects?.each { project ->
                                println "Project: ${project}"
                                if (project.value.type != 'root' && project.value.repo && config.gitlabHelper.isGitlabRepo(project.value.repo, config)) {
                                    def webhook_tags
                                    if (config.params.webhooksEnvironments) {
                                        webhook_tags = config.params.webhooksEnvironments
                                    }
                                    else if (config.webhooksEnvironments) {
                                        webhook_tags = config.webhooksEnvironments
                                    }
                                    println "Webhook Tags: ${webhook_tags}"
                                    if (webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                                        def tag_servers = config.dslHelper.getServersByTags(webhook_tags, config.jenkinsServers)
                                        config.gitlabHelper.deleteWebhook(
                                            project.value.repo,
                                            tag_servers,
                                            "project/${config.jenkinsFolderName}/${currentName}"
                                        )
                                        for (jenkinsServer in tag_servers) {
                                            config.gitlabHelper.addWebhook(
                                                project.value.repo,
                                                jenkinsServer.value.jenkinsUrl.substring(0, jenkinsServer.value.jenkinsUrl.length() - (jenkinsServer.value.jenkinsUrl.endsWith("/") ? 1 : 0)) + '/' + "project/${config.jenkinsFolderName}/${currentName}"
                                            )
                                        }
                                        println "Webhook added for project ${project}"
                                    }
                                }
                            }
                        }
                    }
                }
                else if (config.tags && config.tags.contains('single') && config.components && config.states && config.states.containsKey(state) && config.components.containsKey('master') && config.components.master.containsKey('states') && config.components.master.states.containsKey(state) && config.components.master.states[state].containsKey('version')) {
                    if (config.env.GITLAB_API_TOKEN_TEXT) {
                        if (job.value.containsKey('webhook_trigger') && (job.value.webhook_trigger == 0 || job.value.webhook_trigger == '0' || job.value.webhook_trigger == false || job.value.webhook_trigger == 'false')) {
                            println "Webhooks creation disabled by webhook_trigger configuration option"
                        }
                        else {
                            println "Processing Gitlab webhooks for Single project type"
                            println "Project: master"
                            if (config.gitlabHelper.isGitlabRepo(config.configRepo, config)) {
                                def webhook_tags
                                if (config.params.webhooksEnvironments) {
                                    webhook_tags = config.params.webhooksEnvironments
                                }
                                else if (config.webhooksEnvironments) {
                                    webhook_tags = config.webhooksEnvironments
                                }
                                println "Webhook Tags: ${webhook_tags}"
                                if (webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                                    def tag_servers = config.dslHelper.getServersByTags(webhook_tags, config.jenkinsServers)
                                    config.gitlabHelper.deleteWebhook(
                                        config.configRepo,
                                        tag_servers,
                                        "project/${config.jenkinsFolderName}/${currentName}"
                                    )
                                    for (jenkinsServer in tag_servers) {
                                        config.gitlabHelper.addWebhook(
                                            config.configRepo,
                                            jenkinsServer.value.jenkinsUrl.substring(0, jenkinsServer.value.jenkinsUrl.length() - (jenkinsServer.value.jenkinsUrl.endsWith("/") ? 1 : 0)) + '/' + "project/${config.jenkinsFolderName}/${currentName}"
                                        )
                                    }
                                    println "Webhook added for project master"
                                }
                            }
                        }
                    }
                }
            }
            else if (job.value.type == 'release-deploy') {
                def seedRepo = config.configRepo
                def localConfig = config.clone()
                if (job.value.context) {
                    localConfig = config.dslHelper.merge(localConfig, job.value.context)
                }
                String pipelineScriptName = config.dslHelper.getPipelineScriptName()
                String pipelinesRepo = config.dslHelper.getPipelineRepo(localConfig, job)
                String pipelineScriptDirPath = config.dslHelper.getPipelineScriptDirPath(localConfig, job)
                String pipelineScriptDirPathPrefix = (pipelineScriptDirPath && pipelineScriptDirPath.length() > 0) ? "${pipelineScriptDirPath}/" : ""
                String pipelineScriptPath = (config.config_dir && config.config_dir.length() > 0) ? "${pipelineScriptDirPathPrefix}${config.config_dir}/${pipelineScriptName}" : "${pipelineScriptDirPathPrefix}${pipelineScriptName}"
                if (config.pipeline_script_full) {
                    pipelineScriptPath = "${config.pipeline_script_full}"
                }
                println "pipelinesRepo: ${pipelinesRepo}"
                println "pipelineScriptPath: ${pipelineScriptPath}"
                println "pipelineScriptDirPath: ${pipelineScriptDirPath}"
                pipelineJob(currentName) {
                    concurrentBuild(false)
                    logRotator(-1, config.logRotatorNumToKeep)
                    parameters {
                        config.docmanConfig.projects?.each { project ->
                            if ((project.value.type == 'root' || project.value.type == 'root_chain' || project.value.type == 'single') && (project.value.repo || project.value.root_repo)) {

                                if (job.value.source.type == 'tags') {
                                    config.dslParamsHelper.drupipeParamTagsSelectsDeploy(delegate, job, config, 'release', project)
                                }
                                else if (job.value.source.type == 'branches') {
                                    config.dslParamsHelper.drupipeParamBranchesSelectsDeploy(delegate, job, config, 'release', project)
                                }

                                config.dslParamsHelper.drupipeParamOperationsCheckboxes(delegate, job, config)

                                stringParam('environment', job.value.env)
                            }
                        }
                        config.dslParamsHelper.drupipeParamsDefault(delegate, job, config)
                    }
                    definition {
                        cpsScm {
                            scm {
                                git() {
                                    branch('master')
                                    remote {
                                        name('origin')
                                        url(pipelinesRepo)
                                        credentials(config.credentialsId)
                                    }
                                    if (pipelineScriptDirPath && pipelineScriptDirPath.length() > 0) {
                                        extensions {
                                            relativeTargetDirectory(pipelineScriptDirPath)
                                        }
                                    }
                                }
                                scriptPath(pipelineScriptPath)
                            }
                        }
                    }
                }
            }
            else if (job.value.type == 'common') {
                def seedRepo = config.configRepo
                def localConfig = config.clone()
                if (job.value.context) {
                    localConfig = config.dslHelper.merge(localConfig, job.value.context)
                }
//                println "Local config: ${localConfig}"
                // Define repository that will be used for pipelines retrieve and execution.
                String pipelinesRepo = localConfig.configRepo
                // Define path to pipeline script.
                String pipelineScriptPath
                final MODE_CONFIG_ONLY_REPO = 1
                final MODE_CONFIG_AND_PROJECT_REPO = 2
                final MODE_PROJECT_ONLY_REPO = 3
                String configMode = MODE_CONFIG_ONLY_REPO

                println "LOCAL CONFIG pipelines_repo: ${localConfig.pipelines_repo}"
                if (localConfig.pipelines_repo) {
                    pipelinesRepo = localConfig.pipelines_repo
                    pipelineScriptPath = "${pipelineScript}.groovy"
                }
                else {
                    if (job.value.configRepo) {
                        pipelinesRepo = job.value.configRepo
                    }
                }
                if (config.pipeline_script_full) {
                    pipelineScriptPath = "${config.config_dir}/${config.pipeline_script_full}"
                }
                else {
                    if (pipelinesRepo == seedRepo) {
//                    pipelineScriptPath = "${localConfig.projectConfigPath}/${pipelineScript}.groovy"
                        pipelineScriptPath = "${pipelineScript}.groovy"
                    }
                    else {
                        configMode = MODE_CONFIG_AND_PROJECT_REPO
                        pipelineScriptPath = "${pipelineScript}.groovy"
                    }
                }
                println "pipelinesRepo: ${pipelinesRepo}"
                println "pipelineScriptPath: ${pipelineScriptPath}"

                def br = job.value.branch ? job.value.branch : 'master'

                pipelineJob("${currentName}") {
                    concurrentBuild(false)
                    logRotator(-1, config.logRotatorNumToKeep)
                    parameters {
                        if (config.config_version < 2) {
                            // TODO: check if it can be replaced by pipelinesRepo.
                            stringParam('configRepo', pipelinesRepo)
                        }
                        job.value.params?.each { key, value ->
                            stringParam(key, value)
                        }
                        config.dslParamsHelper.drupipeParamsDefault(delegate, job, config)
                    }
                    definition {
                        cpsScm {
                            scm {
                                git() {
                                    remote {
                                        name('origin')
                                        url(pipelinesRepo)
                                        credentials(config.credentialsId)
                                    }
                                    if (job.value.repoDir) {
                                        extensions {
                                            relativeTargetDirectory(job.value.repoDir)
                                        }
                                    }
                                    else if (!job.value.configRepo && config.config_version < 2) {
                                        extensions {
                                            relativeTargetDirectory(config.projectConfigPath)
                                        }
                                    }
                                    branch(br)
                                }
                                scriptPath(pipelineScriptPath)
                            }
                        }
                    }

                    // Configure triggers.
                    if (job.value.triggers) {
                        triggers {
                            if (job.value.triggers.gitlabPush) {
                                gitlabPush {
                                    if (job.value.triggers.gitlabPush.containsKey('buildOnPushEvents')) {
                                        buildOnPushEvents(job.value.triggers.gitlabPush.buildOnPushEvents)
                                    }
                                    if (job.value.triggers.gitlabPush.containsKey('buildOnMergeRequestEvents')) {
                                        buildOnMergeRequestEvents(job.value.triggers.gitlabPush.buildOnMergeRequestEvents)
                                    }
                                    if (job.value.triggers.gitlabPush.containsKey('enableCiSkip')) {
                                        enableCiSkip(job.value.triggers.gitlabPush.enableCiSkip)
                                    }
                                    if (job.value.triggers.gitlabPush.containsKey('rebuildOpenMergeRequest')) {
                                        rebuildOpenMergeRequest(job.value.triggers.gitlabPush.rebuildOpenMergeRequest)
                                    }
                                    if (job.value.triggers.gitlabPush.containsKey('includeBranches')) {
                                        includeBranches(job.value.triggers.gitlabPush.includeBranches.join(', '))
                                    }
                                }
                            }
                        }
                    }

                    // TODO: use "triggers" config parent.
                    if (job.value.containsKey('cron') && job.value.cron instanceof CharSequence) {
                        triggers {
                            cron(job.value.cron)
                        }
                    }
                    def webhook_tags
                    if (config.params.webhooksEnvironments) {
                        webhook_tags = config.params.webhooksEnvironments
                    }
                    else if (config.webhooksEnvironments) {
                        webhook_tags = config.webhooksEnvironments
                    }
                    println "Webhook Tags: ${webhook_tags}"
                    if (job.value.webhooks && webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                        properties {
                            gitLabConnectionProperty {
                                gitLabConnection('Gitlab')
                            }
                        }
                    }
                }

                def webhook_tags
                if (config.params.webhooksEnvironments) {
                    webhook_tags = config.params.webhooksEnvironments
                }
                else if (config.webhooksEnvironments) {
                    webhook_tags = config.webhooksEnvironments
                }
                println "config.webhooksEnvironments: ${config.webhooksEnvironments}"
                println "config.params.webhooksEnvironments: ${config.params.webhooksEnvironments}"
                println "job.value.webhooks: ${job.value.webhooks}"
                println "webhook_tags: ${webhook_tags}"
                println "config.env.drupipeEnvironment: ${config.env.drupipeEnvironment}"
                println "config.jenkinsServers: ${config.jenkinsServers}"
                if (job.value.webhooks && webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                    println "Create webhooks..."
                    job.value.webhooks.each { hook ->
                        def tag_servers = config.dslHelper.getServersByTags(webhook_tags, config.jenkinsServers)
                        config.gitlabHelper.deleteWebhook(
                            pipelinesRepo,
                            tag_servers,
                            "project/${config.jenkinsFolderName}/${currentName}"
                        )
                        for (jenkinsServer in tag_servers) {
                            config.gitlabHelper.addWebhook(
                                // TODO: check if this is OK.
                                //project.value.repo,
                                pipelinesRepo,
                                jenkinsServer.value.jenkinsUrl.substring(0, jenkinsServer.value.jenkinsUrl.length() - (jenkinsServer.value.jenkinsUrl.endsWith("/") ? 1 : 0)) + '/' + "project/${config.jenkinsFolderName}/${currentName}",
                                hook
                            )
                        }
                        println "Webhook added for project ${config.jenkinsFolderName}/${currentName}"
                    }
                }
                else {
                    println "Webhooks weren't created"
                }

            }
            else if (job.value.type == 'selenese') {
//                def repo = config.params.action.SeleneseTester.repoAddress
                def b = config.params.action.SeleneseTester.reference ? config.params.action.SeleneseTester.reference : 'master'

                if (config.env.GITLAB_API_TOKEN_TEXT) {
                    users = config.gitlabHelper.getUsers(repo)
                    println "USERS: ${users}"
                }

                pipelineJob("${currentName}") {
                    concurrentBuild(false)
                    logRotator(-1, config.logRotatorNumToKeep)
                    parameters {
                        stringParam('configRepo', repo)
                        config.dslParamsHelper.drupipeParamsDefault(delegate, job, config)

                        // TODO: move to helper function & auto dsl format.
                        activeChoiceParam('suites') {
                            description('Select one or more suites. If you see the empty list - please re-save the job (related to bug: https://issues.jenkins-ci.org/browse/JENKINS-42655)')
                            filterable()
                            choiceType('MULTI_SELECT')
                            scriptlerScript ('choices.groovy') {
                                def choices_param = job.value.suites.join('|')
                                println "SUITES CHOICES: ${choices_param}"
                                parameter('defaultChoice', choices_param)
                                parameter('choices', choices_param)
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
            else if (job.value.type == 'trigger_all') {
                freeStyleJob("${currentName}") {
                    concurrentBuild(false)
                    logRotator(-1, config.logRotatorNumToKeep)
                    parameters {
                        stringParam('debugEnabled', '0')
                        stringParam('configRepo', config.configRepo)
                        for (jobInFolder in jobs)  {
                            if (jobInFolder.value.jobs) {
                                println "Skip job with chilldren."
                            }
                            else if (jobInFolder.value.type == 'trigger_all') {
                                println "Skip trigger_all job."
                            }
                            else if (jobInFolder.value.type == 'multistep_all') {
                                println "Skip multistep_all job."
                            }
                            else {
                                jobInFolder.value.params?.each { key, value ->
                                    def job_prefix = jobInFolder.key.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase()
                                    def prefixed_key = job_prefix + '_' + key
                                    stringParam(prefixed_key, value)
                                }
                            }
                        }
                    }
                    publishers {
                        downstreamParameterized {
                            for (jobInFolder in jobs)  {
                                if (jobInFolder.value.jobs) {
                                    println "Skip job with chilldren."
                                }
                                else if (jobInFolder.value.type == 'trigger_all') {
                                    println "Skip trigger_all job."
                                }
                                else if (jobInFolder.value.type == 'multistep_all') {
                                    println "Skip multistep_all job."
                                }
                                else {
                                    def jobInFolderName = currentFolder ? "${config.jenkinsFolderName}/${currentFolder}/${jobInFolder.key}" : jobInFolder.key
                                    println "ADD TRIGGER JOB: ${jobInFolderName}"
                                    trigger(jobInFolderName) {
                                        condition("ALWAYS")
                                        parameters {
                                            predefinedProp('debugEnabled', '${debugEnabled}')
                                            predefinedProp('configRepo', '${configRepo}')
                                            jobInFolder.value.params?.each { key, value ->
                                                def job_prefix = jobInFolder.key.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase()
                                                def prefixed_key = job_prefix + '_' + key
                                                predefinedProp(key, '${' + prefixed_key + '}')
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (job.value.type == 'multistep_all') {
                freeStyleJob("${currentName}") {
                    concurrentBuild(false)
                    logRotator(-1, config.logRotatorNumToKeep)
                    wrappers {
                        timestamps()
                    }
                    parameters {
                        stringParam('debugEnabled', '0')
                        stringParam('configRepo', config.configRepo)
                        job.value.params?.each { key, value ->
                            stringParam(key, value)
                        }
                    }
                    steps {
                        downstreamParameterized {
                            for (jobInFolder in jobs)  {
                                if (jobInFolder.value.jobs) {
                                    println "Skip job with chilldren."
                                }
                                else if (jobInFolder.value.type == 'trigger_all') {
                                    println "Skip trigger_all job."
                                }
                                else if (jobInFolder.value.type == 'multistep_all') {
                                    println "Skip multistep_all job."
                                }
                                else {
                                    def jobInFolderName = currentFolder ? "${config.jenkinsFolderName}/${currentFolder}/${jobInFolder.key}" : jobInFolder.key
                                    println "ADD PHASE JOB: ${jobInFolderName}"
                                    trigger(jobInFolderName) {
                                        block {
                                            buildStepFailure('FAILURE')
                                            failure('FAILURE')
                                            unstable('UNSTABLE')
                                        }
                                        parameters {
                                            currentBuild()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (job.value.jobs) {
//            println "Parent config params: ${parentConfigParams}"
            processJob(job.value.jobs, currentName, config)
        }
    }
}
