package com.github.aroq.drupipe.providers.config

import com.github.aroq.drupipe.DrupipeConfig
import com.github.aroq.drupipe.DrupipeController

class ConfigProviderBase implements ConfigProvider, Serializable {

    com.github.aroq.drupipe.Utils utils

    def script

    DrupipeConfig config

    DrupipeController controller

    def provide() {

    }

}
