package com.github.aroq.drupipe.providers.config

class ConfigProviderMothership extends ConfigProviderBase {

    def provide() {
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

            drupipeConfig.projects = mothershipConfig

            def mothershipServers = getMothershipServersFile(result)
            controller.drupipeLogger.debugLog(drupipeConfig.config, mothershipServers, 'mothershipServers', [debugMode: 'json'])

            def mothershipProjectConfig = mothershipConfig[drupipeConfig.config.jenkinsFolderName]
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

}
