package com.github.aroq.drupipe.providers.config

import com.github.aroq.drupipe.DrupipeController

class ConfigProviderBase implements ConfigProvider, Serializable {

    com.github.aroq.drupipe.Utils utils

    def script

    def config

    DrupipeController controller

    def provide() {

    }

}
