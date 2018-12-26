package com.github.aroq.drupipe.providers.config

import com.github.aroq.drupipe.DrupipeConfig
import com.github.aroq.drupipe.DrupipeController

class ConfigProviderBase implements ConfigProvider, Serializable {

    public com.github.aroq.drupipe.Utils utils

    public def script

    public DrupipeConfig drupipeConfig

    public DrupipeController controller

    public String configCachePath
    public String configFileName

    def result

    boolean saveCache

    def provide() {
        _init()
        script.echo "FORCE: ${script.env.JENKINS_HOME}"
        if (script.env.JENKINS_HOME != '1' && configFileName && this.script.fileExists(configFileName)) {
            script.echo "Cached Config is found, loading: " + configFileName
            result = script.readYaml(file: configFileName)
        }
        else {
            script.echo "Cached Config is not found: " + configFileName
            result = _provide()
            saveCache = true
        }
        _finalize()

        return result
    }

    def _init() {
        saveCache = false
        configCachePath = ""
        configFileName = ""
    }

    def _provide() {
    }

    def _finalize() {
        if (saveCache && configCachePath && configFileName) {
            script.sh("mkdir -p ${configCachePath}")
            controller.serializeObject(configFileName, result)
        }
    }
}
