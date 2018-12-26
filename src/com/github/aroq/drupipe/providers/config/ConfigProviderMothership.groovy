package com.github.aroq.drupipe.providers.config

class ConfigProviderMothership extends ConfigProviderBase {

    def _init() {
        configCachePath = script.env.JENKINS_HOME + "/config_cache"
        configFileName = configCachePath + "/ConfigProviderMothership.yaml"
    }

    def _provide() {
        def result = [:]
        if (drupipeConfig.config.env.MOTHERSHIP_REPO) {
            def source = [
                name:          'mothership',
                type:          'git',
                path:          '.unipipe/mothership',
                url:           drupipeConfig.config.env.MOTHERSHIP_REPO,
                branch:        'master',
                credentialsId: drupipeConfig.config.env.credentialsId,
            ]
            drupipeConfig.drupipeSourcesController.sourceAdd(source)
            result = drupipeConfig.drupipeSourcesController.sourceLoad(
                sourceName: 'mothership',
                configType: 'groovy',
                configPath: 'mothership.config',
            )
            controller.drupipeLogger.debugLog(drupipeConfig.config, result, 'mothershipConfig: result', [debugMode: 'json'])

            def mothershipConfig = getMothershipConfigFile(result)
            controller.drupipeLogger.debugLog(drupipeConfig.config, mothershipConfig, 'mothershipConfig', [debugMode: 'json'])

            def envMothershipConfig = getEnvMothershipConfigFile(result)
            controller.drupipeLogger.debugLog(drupipeConfig.config, envMothershipConfig, 'envMothershipConfig', [debugMode: 'json'])
            if (envMothershipConfig) {
                mothershipConfig = utils.merge(mothershipConfig, envMothershipConfig)
            }

            drupipeConfig.projects = mothershipConfig

            def projectNames = drupipeConfig.projects.keySet() as ArrayList
            String jobName = script.env.JOB_NAME
            result['jenkinsFolderName'] = utils.getJenkinsFolderName(jobName, projectNames)
            result['jenkinsJobName'] = utils.getJenkinsJobName(jobName, projectNames)

            def mothershipServers = getMothershipServersFile(result)
            controller.drupipeLogger.debugLog(drupipeConfig.config, mothershipServers, 'mothershipServers', [debugMode: 'json'])

            def mothershipProjectConfig = mothershipConfig[result.jenkinsFolderName]
            script.echo "mothershipProjectConfig: ${mothershipProjectConfig}"

            result = utils.merge(result, mothershipProjectConfig)
            controller.drupipeLogger.debugLog(drupipeConfig.config, result, 'mothershipServer result after merge', [debugMode: 'json'])

            result = utils.merge(result, [jenkinsServers: mothershipServers])
            controller.drupipeLogger.debugLog(drupipeConfig.config, result, 'mothershipServer result2 after merge', [debugMode: 'json'])
        }

        result
    }

    def getMothershipServersFile(params) {
        def serversFileName = 'servers'
        def extensions = ['yaml', 'yml']
        def dir = drupipeConfig.drupipeSourcesController.sourceDir(params, 'mothership')
        for (extension in extensions) {
            def serversFile = "${dir}/${serversFileName}.${extension}"
            if (script.fileExists(serversFile)) {
                def file = script.readFile(serversFile)
                if (file) {
                    return script.readYaml(text: file).servers
                }
            }
        }
        throw new Exception("getMothershipServersFile: servers config file not found.")
    }

    def getMothershipConfigFile(params) {
        def projectsFileName = 'projects'
        def extensions = ['yaml', 'yml', 'json']
        def dir = drupipeConfig.drupipeSourcesController.sourceDir(params, 'mothership')
        for (extension in extensions) {
            def projectsFile = "${dir}/${projectsFileName}.${extension}"
            if (script.fileExists(projectsFile)) {
                def file = script.readFile(projectsFile)
                if (file) {
                    if (extension in ['yaml', 'yml']) {
                        return script.readYaml(text: file).projects
                    } else if (extension == 'json') {
                        return script.readJSON(text: file).projects
                    }
                }
            }
            else {
                controller.drupipeLogger.log "getMothershipConfigFile: file doesn't exist"
            }
        }
        throw new Exception("getMothershipConfigFile: mothership config file not found.")
    }

    def getEnvMothershipConfigFile(params) {
        def projectsFileName = 'projects'
        def extensions = ['yaml', 'yml', 'json']
        def dir = drupipeConfig.drupipeSourcesController.sourceDir(params, 'mothership')
        for (extension in extensions) {
            def projectsFile = "${dir}/${drupipeConfig.config.env.drupipeEnvironment}.${projectsFileName}.${extension}"
            if (script.fileExists(projectsFile)) {
                def file = script.readFile(projectsFile)
                if (file) {
                    if (extension in ['yaml', 'yml']) {
                        return script.readYaml(text: file).projects
                    } else if (extension == 'json') {
                        return script.readJSON(text: file).projects
                    }
                }
            }
            else {
                controller.drupipeLogger.log "getEnvMothershipConfigFile: file doesn't exist"
            }
        }
        return null
    }

}
