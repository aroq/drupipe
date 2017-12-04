@Grab(group='org.yaml', module='snakeyaml', version='1.18')
import org.yaml.snakeyaml.Yaml
import com.github.aroq.dsl.GitlabHelper
import com.github.aroq.dsl.DslHelper

def dslHelper = new DslHelper(script: this)
def configMain = dslHelper.readJson(this, '.unipipe/temp/context_processed.json')

println "configMain: ${configMain}"

def projectsFileRead(filePath) {
  try {
      return readFileFromWorkspace(filePath)
  }
  catch(e) {
      return null
  }
}

def projects = [:]
def projectsFileName = 'projects'
def fileExtensions = ['yaml', 'yml', 'json']

for (extension in fileExtensions) {
    def projectsFile = projectsFileName + '.' + extension
    def file = projectsFileRead(projectsFile)
    if (file) {
        println "Using ${projectsFile}"
        println file
        if (extension in ['yaml', 'yml']) {
            Yaml yaml = new Yaml()
            def config = yaml.load(file)
            projects = config.projects
            break
        }
        else if (extension == 'json') {
            projects = JsonSlurper.newInstance().parseText(json_file).projects
            break
        }
    }
}

if (projects.size() == 0) {
    println "Projects empty. Check configuration file projects.(yaml|yml|json)."
}

println 'Projects: ' + projects.keySet().join(', ')

def servers = [:]
def serversFileNames = ['servers.yaml', 'servers.yml']
for (serversFileName in serversFileNames) {
    file = projectsFileRead(serversFileName)
    if (file) {
        println "Using ${serversFileName}"
        println file
        Yaml yaml = new Yaml()
        def servers_config = yaml.load(file)
        servers = servers_config.servers
        break
    }
}

if (servers.size() == 0) {
    println "Servers empty. Check configuration file servers.(yaml|yml)."
}

println 'Servers: ' + servers.keySet().join(', ')

def gitlabHelper = new GitlabHelper(script: this, config: configMain)

projects.each { project ->
    println "PROJECT: ${project.value}"
    def config = configMain.clone()
    config = merge(config, project.value)
    def jenkins_servers
    if (config.params.jenkinsServers) {
        jenkins_servers = config.params.jenkinsServers
    }
    if (jenkins_servers && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && jenkins_servers.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
        println "CONFIG projectConfigPath: ${config.projectConfigPath}"
        println "CONFIG mothership_job_name: ${config.mothership_job_name}"
        println "CONFIG mothership_job_jenkinsfile: ${config.mothership_job_jenkinsfile}"

        String jenkinsfile = config.mothership_job_jenkinsfile ? config.mothership_job_jenkinsfile : 'Jenkinsfile'

        String subDir = ''
        if (config.containsKey('config_version') && config.config_version == 2) {
            if (config.config_dir) {
                subDir = config.config_dir
                pipelineScriptPath = "${config.config_dir}/${jenkinsfile}"
            }
            else {
                pipelineScriptPath = "${jenkinsfile}"
            }
        }
        else {
            subDir = config.projectConfigPath ? config.projectConfigPath.substring(0, config.projectConfigPath.length() - (config.projectConfigPath.endsWith("/") ? 1 : 0)) + '/' : ''
            pipelineScriptPath = "${subDir}${jenkinsfile}"
        }

        if (config.mothership_job_type == 'Jenkinsfile') {
            String jobName = config.mothership_job_name ? config.mothership_job_name : project.key
            println "JOB NAME: ${jobName}"
            def users = [:]

            // TODO: Add condition checking if permissions should be set based on Gitlab permissions.
            // TODO: Add condition checking if repo is in Gitlab.
            if (config.env.GITLAB_API_TOKEN_TEXT && !config.noHooks) {
                users = gitlabHelper.getUsers(config.configRepo)
                println "USERS: ${users}"
            }

            println "FOLDER: ${project.key}"
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
                            println "Added UPDATE/BUILD/CANCEL permissions for user:${user.key}, folder: ${project.key}"
                        }
                    }
                }
            }

            pipelineJob("${project.key}/${jobName}") {
                concurrentBuild(false)
                logRotator(-1, config.logRotatorNumToKeep)
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
                                    if (subDir) {
                                        relativeTargetDirectory(subDir)
                                    }
                                }
                            }
                            scriptPath(pipelineScriptPath)
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
                if (!config.noHooks && webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                    triggers {
                        if (config.env.GITLAB_API_TOKEN_TEXT) {
                            gitlabPush {
                                buildOnPushEvents()
                                buildOnMergeRequestEvents(false)
                                enableCiSkip()
                                useCiFeatures()
                                includeBranches('master')
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
            if (config.env.GITLAB_API_TOKEN_TEXT && !config.noHooks) {
                def webhook_tags
                if (config.params.webhooksEnvironments) {
                    webhook_tags = config.params.webhooksEnvironments
                }
                else if (config.webhooksEnvironments) {
                    webhook_tags = config.webhooksEnvironments
                }
                println "Webhook Tags: ${webhook_tags}"
                if (webhook_tags && config.jenkinsServers.containsKey(config.env.drupipeEnvironment) && config.jenkinsServers[config.env.drupipeEnvironment].containsKey('tags') && webhook_tags.intersect(config.jenkinsServers[config.env.drupipeEnvironment].tags)) {
                    def tag_servers = getServersByTags(webhook_tags, servers)
                    gitlabHelper.deleteWebhook(
                        config.configRepo,
                        tag_servers,
                        "project/${project.key}/seed"
                    )
                    for (jenkinsServer in tag_servers) {
                        gitlabHelper.addWebhook(
                            config.configRepo,
                            jenkinsServer.value.jenkinsUrl.substring(0, jenkinsServer.value.jenkinsUrl.length() - (jenkinsServer.value.jenkinsUrl.endsWith("/") ? 1 : 0)) + '/' + "project/${project.key}/seed"
                        )
                    }
                }
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
}

Map merge(Map[] sources) {
    if (sources.length == 0) return [:]
    if (sources.length == 1) return sources[0]

    sources.inject([:]) { result, source ->
        if (source && source.containsKey('override') && source['override']) {
            result = source
        }
        else {
            source.each { k, v ->
                if (result[k] instanceof Map && v instanceof Map ) {
                    if (v.containsKey('override') && v['override']) {
                        v.remove('override')
                        result[k] = v
                    }
                    else {
                        result[k] = merge(result[k], v)
                    }
                }
                else if (result[k] instanceof List && v instanceof List) {
                    result[k] += v
                    result[k] = result[k].unique()
                }
                else {
                    result[k] = v
                }
            }
        }
        result
    }
}

def getServersByTags(tags, servers) {
    def result = [:]
    if (tags && tags instanceof ArrayList) {
        for (def i = 0; i < tags.size(); i++) {
            def tag = tags[i]
            for (server in servers) {
                if (server.value?.tags && tag in server.value?.tags && server.value?.jenkinsUrl) {
                    result << ["${server.key}": server.value]
                }
            }
        }
    }
    println "getServersByTags: ${result}"
    result
}
