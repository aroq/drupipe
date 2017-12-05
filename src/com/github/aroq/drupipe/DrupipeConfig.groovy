package com.github.aroq.drupipe

import com.github.aroq.drupipe.processors.DrupipeProcessor
import com.github.aroq.drupipe.processors.DrupipeProcessorsController
import com.github.aroq.drupipe.providers.config.ConfigProvider

class DrupipeConfig implements Serializable {

    DrupipeController controller

    def script

    com.github.aroq.drupipe.Utils utils

    def config

    ArrayList<ConfigProvider> configProviders = []

    DrupipeSourcesController drupipeSourcesController

    def projects

    def config(params, parent) {
        drupipeSourcesController = new DrupipeSourcesController(script: script, utils: utils, controller: controller)
        script.node('master') {
            this.script.sh("mkdir -p .unipipe")
            this.script.sh("mkdir -p .unipipe/temp")

            params.debugEnabled = params.debugEnabled && params.debugEnabled != '0' ? true : false
            utils.dump(params, params, 'PIPELINE-PARAMS')

            config = script.readYaml(text: script.libraryResource('com/github/aroq/drupipe/config.yaml'))
            config = utils.merge(config, script.readYaml(text: script.libraryResource('com/github/aroq/drupipe/actions.yaml')))
            config.jenkinsParams = params

            // TODO: remove it when all configs are updated to version 2.
            if (script.env.JOB_NAME == 'mothership') {
                config.config_version = 2
            }

            // TODO: Perform SCM checkout only when really needed.
            this.script.checkout this.script.scm

            // Get config from config providers.
            for (def i = 0; i < config.config_providers_list.size(); i++) {
                def properties = [script: script, utils: utils, drupipeConfig: this, controller: controller]
                def className = "com.github.aroq.drupipe.providers.config.${config.config_providers[config.config_providers_list[i]].class_name}"
                script.echo "Config Provider class name: ${className}"
                configProviders.add(parent.class.classLoader.loadClass(className, true, false)?.newInstance(
                    properties
                ))
            }
            for (def i = 0; i < configProviders.size(); i++) {
                ConfigProvider configProvider = configProviders[i]
                config = utils.merge(config, configProvider.provide())
            }

            // TODO: remove it when all configs are updated to version 2.
            // For compatibility:
            if (config.defaultActionParams) {
                config.params.actions = utils.merge(config.params.actions, config.defaultActionParams)
            }

            // TODO: Refactor it.
            config.environmentParams = [:]
            if (config.environments) {
                if (config.environment) {
                    def environment = config.environments[config.environment]
                    if (config.servers && environment['server'] && config.servers[environment['server']]) {
                        def server = config.servers[environment['server']]
                        config.environmentParams = utils.merge(server, environment)
                    }
                    else {
                        config.environmentParams = environment
                    }
                    // For compatibility:
                    if (config.environmentParams && config.environmentParams.defaultActionParams) {
                        config.params.action = utils.merge(config.params.action, config.environmentParams.defaultActionParams)
                    }

                    utils.debugLog(config, config.environmentParams, 'ENVIRONMENT PARAMS', [:], [], true)
                }
                else {
                    script.echo "No context.environment is defined"
                }
            }
            else {
                script.echo "No context.environments are defined"
            }

            utils.debugLog(config, config, 'CONFIG CONTEXT')

            def stashes = config.loadedSources.collect { k, v -> v.path + '/**'}.join(', ')

            script.echo "Stashes: ${stashes}"

            script.stash name: 'config', includes: "${stashes}", excludes: ".git, .git/**"

            controller.archiveObjectJsonAndYaml(config, 'context')
        }
        return config
    }

    DrupipeProcessorsController initProcessorsController(parent, processorsConfig) {
        utils.log "initProcessorsController"
        ArrayList<DrupipeProcessor> processors = []
        for (processorConfig in processorsConfig) {
            utils.log "Processor: ${processorConfig.className}"
            try {
                def properties = [utils: utils]
                if (processorConfig.properties) {
                    properties << processorConfig.properties
                }
                processors << parent.class.classLoader.loadClass("com.github.aroq.drupipe.processors.${processorConfig.className}", true, false)?.newInstance(
                    properties
                )
                utils.log "Processor: ${processorConfig.className} created"
            }
            catch (err) {
                throw err
            }
        }
        new DrupipeProcessorsController(processors: processors, utils: utils)
    }

    def processItem(item, parentKey, paramsKey = 'params', mode) {
        utils.log "DrupipeConfig->processItem"
        controller.drupipeProcessorsController.process(config, item, parentKey, paramsKey, mode)
    }

    def process() {
        utils.log "DrupipeConfig->process()"
        if (controller.configVersion() > 1) {
//            controller.drupipeProcessorsController = initProcessorsController(this, config.processors)
            if (config.jobs) {
                config.jobs = processItem(config.jobs, 'context', 'params', 'config')
            }
            controller.archiveObjectJsonAndYaml(config, 'context_processed')
        }
    }

    int configVersion() {
        config.config_version as int
    }

    def config_version2() {
        utils.log "DrupipeConfig->config_version2()"
        script.readYaml(text: script.libraryResource('com/github/aroq/drupipe/config_version2.yaml'))
    }

    @NonCPS
    def groovyConfig(text) {
        new HashMap<>(ConfigSlurper.newInstance(script.env.drupipeEnvironment).parse(text))
    }


}
