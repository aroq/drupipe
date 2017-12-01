package com.github.aroq.drupipe.providers.config

class ConfigProviderConfigFile extends ConfigProviderBase {

    def provide() {
        def providers = [
            [
                action: 'YamlFileConfig.loadFromLibraryResource',
                params: [
                    resource: 'com/github/aroq/drupipe/config.yaml'
                ]
            ],
        ]

        controller.executePipelineActionList(providers)
    }

}
