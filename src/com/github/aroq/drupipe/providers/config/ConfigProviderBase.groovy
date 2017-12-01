package com.github.aroq.drupipe.providers.config

import com.github.aroq.drupipe.DrupipeController

class ConfigProviderBase implements ConfigProvider, Serializable {

    public com.github.aroq.drupipe.Utils utils

    public def script

    public def config

    public DrupipeController controller

    def provide() {

    }

}
