package com.github.aroq.drupipe.providers.config

class ConfigProviderMothership extends ConfigProviderBase {

    def provide() {
        def result = [:]
        if (drupipeConfig.config.env.MOTHERSHIP_REPO) {
            def sourceObject = [
                name:   'mothership',
                type:   'git',
                path:   '.unipipe/mothership',
                url:    drupipeConfig.config.env.MOTHERSHIP_REPO,
                branch: 'master',
            ]
            drupipeConfig.drupipeSourcesController.sourceAdd(credentialsId: drupipeConfig.config.env.credentialsId, source: sourceObject)
            result = drupipeConfig.drupipeSourcesController.sourceLoad(
                sourceName: 'mothership',
                configType: 'groovy',
                configPath: 'mothership.config',
            )
//            this.script.drupipeAction([action: "Source.add", params: [credentialsId: drupipeConfig.config.env.credentialsId, source: sourceObject]], controller)

//            def providers = [
//                [
//                    action: 'Source.add',
//                    params: [
//                        source: sourceObject,
//                        credentialsId: drupipeConfig.config.env.credentialsId,
//                    ],
//                ],
//                [
//                    action: 'Source.loadConfig',
//                    params: [
//                        sourceName: 'mothership',
//                        configType: 'groovy',
//                        configPath: 'mothership.config',
//                    ]
//                ]
//            ]
//            result = controller.executePipelineActionList(providers)
            def mothershipConfig = this.utils.getMothershipConfigFile(result)
            utils.debugLog(drupipeConfig.config, mothershipConfig, 'mothershipConfig', [debugMode: 'json'], [], false)
            def mothershipServers = this.utils.getMothershipServersFile(result)
            utils.debugLog(drupipeConfig.config, mothershipServers, 'mothershipServers', [debugMode: 'json'], [], false)

            def mothershipProjectConfig = mothershipConfig[drupipeConfig.config.jenkinsFolderName]
            script.echo "mothershipProjectConfig: ${mothershipProjectConfig}"

            result = utils.merge(result, mothershipProjectConfig)
            utils.debugLog(drupipeConfig.config, result, 'mothershipServer result after merge', [debugMode: 'json'], [], false)
            result = utils.merge(result, [jenkinsServers: mothershipServers])
            utils.debugLog(drupipeConfig.config, result, 'mothershipServer result2 after merge', [debugMode: 'json'], [], false)

            if (result.config_version > 1) {
                utils.log "Initialising drupipeProcessorsController"
                controller.drupipeProcessorsController = controller.drupipeConfig.initProcessorsController(this, drupipeConfig.config.processors)
            }
        }

        if (result.config_version && result.config_version > 1 || controller.configVersion() > 1) {
            result = utils.merge(controller.drupipeConfig.config_version2(), result)
        }

        result
    }

}
