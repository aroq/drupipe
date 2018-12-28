package com.github.aroq.drupipe.providers.config

class ConfigProviderEnv extends ConfigProviderBase {

    def _init() {
        super._init()
        controller.drupipeLogger.trace "ConfigProviderEnv _init()"
    }

    def _provide() {
        controller.drupipeLogger.trace "ConfigProviderEnv _provide()"
        controller.drupipeLogger.debugLog(drupipeConfig.config, drupipeConfig.config,"drupipeConfig.config: ${drupipeConfig.config}", [:])
        config.workspace = script.pwd()
        config.env = utils.envToMap()

        // TODO: Use env vars pattern to override.
        config.credentialsId = config.env.credentialsId
        config.environment = config.env.environment
        config.configRepo = config.env.configRepo

        if (script.env.KUBERNETES_PORT) {
          config.containerMode = 'kubernetes'
        }
        utils.serializeAndDeserialize(config)
    }

}
