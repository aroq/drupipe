package com.github.aroq.drupipe.providers.config

class ConfigProviderMothership extends ConfigProviderBase {

    def provide() {
        if (drupipeConfig.config) {
            script.echo "OK"
        }
        def result = [:]
        if (this.drupipeConfig.config.env.MOTHERSHIP_REPO) {
            def sourceObject = [
                name:   'mothership',
                type:   'git',
                path:   '.unipipe/mothership',
                url:    script.env.MOTHERSHIP_REPO,
                branch: 'master',
            ]

            this.script.drupipeAction([action: "Source.add", params: [credentialsId: config.env.credentialsId, source: sourceObject]], controller)

            def providers = [
                [
                    action: 'Source.add',
                    params: [
                        source: sourceObject,
                        credentialsId: config.env.credentialsId,
                    ],
                ],
                [
                    action: 'Source.loadConfig',
                    params: [
                        sourceName: 'mothership',
                        configType: 'groovy',
                        configPath: 'mothership.config',
                    ]
                ]
            ]
            result = controller.executePipelineActionList(providers)
            def mothershipConfig = this.utils.getMothershipConfigFile(result)
            utils.debugLog(config, mothershipConfig, 'mothershipConfig', [debugMode: 'json'], [], false)
            def mothershipServers = this.utils.getMothershipServersFile(result)
            utils.debugLog(config, mothershipServers, 'mothershipServers', [debugMode: 'json'], [], false)

            def mothershipProjectConfig = mothershipConfig[config.jenkinsFolderName]
            script.echo "mothershipProjectConfig: ${mothershipProjectConfig}"

            result = utils.merge(result, mothershipProjectConfig)
            utils.debugLog(config, result, 'mothershipServer result after merge', [debugMode: 'json'], [], false)
            result = utils.merge(result, [jenkinsServers: mothershipServers])
            utils.debugLog(config, result, 'mothershipServer result2 after merge', [debugMode: 'json'], [], false)

            if (result.config_version > 1) {
                utils.log "Initialising drupipeProcessorsController"
                controller.drupipeProcessorsController = controller.drupipeConfig.initProcessorsController(this, config.processors)
            }
        }
        result
    }

}
