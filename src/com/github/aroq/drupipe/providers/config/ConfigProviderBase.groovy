package com.github.aroq.drupipe.providers.config

import com.github.aroq.drupipe.DrupipeConfig
import com.github.aroq.drupipe.DrupipeController

class ConfigProviderBase implements ConfigProvider, Serializable {

    public com.github.aroq.drupipe.Utils utils

    public def script

    public DrupipeConfig drupipeConfig

    public DrupipeController controller

    def provide() {

    }

}
