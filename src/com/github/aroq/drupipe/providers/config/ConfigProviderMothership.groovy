package com.github.aroq.drupipe.providers.config

class ConfigProviderMothership extends ConfigProviderBase {

    def _init() {
        super._init()
        script.trace "ConfigProviderMothership _init()"
        configCachePath = script.env.JENKINS_HOME + "/config_cache"
        configFileName = configCachePath + "/ConfigProviderMothership.yaml"
        if (script.env.JOB_NAME == 'mothership') {
            // Clear cached config.
            script.sh("rm -fR ${configCachePath}")
        }
    }

    def _provide() {
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
            config = drupipeConfig.drupipeSourcesController.sourceLoad(
                sourceName: 'mothership',
                configType: 'groovy',
                configPath: 'mothership.config',
            )
            controller.drupipeLogger.debugLog(drupipeConfig.config, config, 'mothershipConfig: config', [debugMode: 'json'])

            def mothershipConfig = getMothershipConfigFile(config)
            controller.drupipeLogger.debugLog(drupipeConfig.config, mothershipConfig, 'mothershipConfig', [debugMode: 'json'])

            def envMothershipConfig = getEnvMothershipConfigFile(config)
            controller.drupipeLogger.debugLog(drupipeConfig.config, envMothershipConfig, 'envMothershipConfig', [debugMode: 'json'])
            if (envMothershipConfig) {
                mothershipConfig = utils.merge(mothershipConfig, envMothershipConfig)
            }

            drupipeConfig.projects = mothershipConfig
            config.projectNames = drupipeConfig.projects.keySet() as ArrayList

            def mothershipServers = getMothershipServersFile(config)
            controller.drupipeLogger.debugLog(drupipeConfig.config, mothershipServers, 'mothershipServers', [debugMode: 'json'])

            def mothershipProjectConfig = mothershipConfig[config.jenkinsFolderName]
            script.echo "mothershipProjectConfig: ${mothershipProjectConfig}"

            config = utils.merge(config, mothershipProjectConfig)
            controller.drupipeLogger.debugLog(drupipeConfig.config, config, 'mothershipServer config after merge', [debugMode: 'json'])

            config = utils.merge(config, [jenkinsServers: mothershipServers])
            controller.drupipeLogger.debugLog(drupipeConfig.config, config, 'mothershipServer config2 after merge', [debugMode: 'json'])
        }

        config
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
