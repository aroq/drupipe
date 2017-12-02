package com.github.aroq.drupipe.providers.config

class ConfigProviderConfigFile extends ConfigProviderBase {

    def provide() {
        script.readYaml(text: script.libraryResource('com/github/aroq/drupipe/config.yaml'))
    }

}
