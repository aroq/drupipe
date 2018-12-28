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

    public config

    def provide() {
        _init()
        if (script.env.force != '1') {
            if (configFileName) {
                if (this.script.fileExists(configFileName)) {
                    controller.drupipeLogger.trace "${this.class.name}: Cached Config is found, loading: " + configFileName
                    result = script.readYaml(file: configFileName)
                }
                else {
                    controller.drupipeLogger.trace "${this.class.name}: Cached Config is not found: " + configFileName
                    result = _provide()
                    saveCache = true
                }
            }
            else {
                controller.drupipeLogger.trace "${this.class.name}: Cached Config is not loaded because configFileName is not set"
            }
        }
        else {
            controller.drupipeLogger.trace "${this.class.name}: Cached Config is not loaded because of FORCE mode enabled"
        }
        _finalize()

        return result
    }

    def _init() {
        config = [:]
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
