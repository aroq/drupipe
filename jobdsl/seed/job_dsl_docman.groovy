import com.github.aroq.DocmanConfig
import com.github.aroq.GitlabHelper

println "Docman Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

docrootConfigJsonPath = config.docrootConfigJsonPath ? config.docrootConfigJsonPath : "${config.projectConfigPath}/config.json"
docrootConfigJson = readFileFromWorkspace(docrootConfigJsonPath)

if (!config.tags || !config.tags.contains('docman')) {
    println "Config: ${config}"

    if (config.configSeedType == 'docman') {
        // Retrieve Docman config from json file (prepared by "docman info" command).
        def docmanConfig = new DocmanConfig(script: this, docrootConfigJson: docrootConfigJson)
        println "Docman config: ${docmanConfig.init()}"

        if (config.env.GITLAB_API_TOKEN_TEXT) {
            println "Initialize Gitlab Helper"
            gitlabHelper = new GitlabHelper(script: this, config: config)
        }

        def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipeline'

// TODO: Use docman config to retrieve info.
        branches = [
            development: [
                pipeline: pipelineScript,
                quietPeriodSeconds: 5,
            ],
            staging: [
                pipeline: pipelineScript,
                quietPeriodSeconds: 5,
            ],
            stable: [
                pipeline: pipelineScript,
                quietPeriodSeconds: 5,
            ],
        ]

        if (config.branches) {
            branches = merge(branches, config.branches)
        }

// Create pipeline jobs for each state defined in Docman config.
        docmanConfig.states?.each { state ->
            def params = config
            println "Processing state: ${state.key}"
            if (branches[state.key]) {
                params = merge(config, branches[state.key])
            }
            if (branches[state.key]?.branch) {
                branch = branches[state.key]?.branch
            }
            else {
                branch = docmanConfig.getVersionBranch('', state.key)
            }
        }

    }
}


def isGitlabRepo(repo, config) {
    config.env.GITLAB_HOST && repo.contains(config.env.GITLAB_HOST)
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
