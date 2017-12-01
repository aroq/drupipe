package com.github.aroq.drupipe.providers.config

class ConfigProviderJenkins extends ConfigProviderBase {

    def provide() {
        action.params.jenkinsParams
    }

}
