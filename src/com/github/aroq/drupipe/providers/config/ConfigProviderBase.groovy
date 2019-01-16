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
        controller.drupipeLogger.log "${this.class.name}: ConfigProviderBase->provide() START"
        _init()
        boolean needProvide = true
        if (script.env.force != '1') {
            if (configFileName) {
                if (this.script.fileExists(configFileName)) {
                    controller.drupipeLogger.log "${this.class.name}: Cached Config is found, loading: " + configFileName
                    result = script.readYaml(file: configFileName)
                    needProvide = false
                }
                else {
                    controller.drupipeLogger.log "${this.class.name}: Cached Config is not found: " + configFileName
                }
            }
            else {
                controller.drupipeLogger.log "${this.class.name}: Cached Config is not loaded because configFileName is not set"
            }
        }
        else {
            controller.drupipeLogger.log "${this.class.name}: Cached Config is not loaded because of FORCE mode enabled"
        }
        if (needProvide) {
            result = _provide()
            saveCache = true
        }
        _finalize()

        controller.drupipeLogger.log "${this.class.name}: ConfigProviderBase->provide() END"
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
        controller.drupipeLogger.log "${this.class.name}: ConfigProviderBase->_finalize() START"
        if (saveCache && configCachePath && configFileName) {
            script.sh("mkdir -p ${configCachePath}")
            controller.serializeObject(configFileName, result)
        }
        controller.drupipeLogger.log "${this.class.name}: ConfigProviderBase->_finalize() END"
    }
}
