package com.github.aroq.drupipe

class DrupipeActionWrapper implements Serializable {

    String action

    String name

    String methodName

    HashMap params = [:]

    HashMap notification = [:]

    DrupipeController pipeline

    def script

    def result = [:]

    // This param should only be used for result storage and will be merged into pipeline.context automatically.
    def context = [:]

    def utils

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute() {
        utils = pipeline.utils

        this.script = pipeline.script

        try {
            // Stage name & echo.
            String drupipeStageName
            if (pipeline.block && pipeline.block.stage) {
                drupipeStageName = "${pipeline.block.stage.name}"
            }
            else {
                drupipeStageName = 'config'
            }
            pipeline.context.drupipeStageName = drupipeStageName
            notification.name = "Action ${name}"
            notification.level = "action:${drupipeStageName}"

            utils.pipelineNotify(pipeline.context, notification << [status: 'START'])
            utils.echoDelimiter("-----> DrupipeStage: ${drupipeStageName} | DrupipeActionWrapper name: ${this.fullName} start <-")


            // Define action params.
            def actionParams = [:]
            def defaultActionParams = [:]

            // TODO: read action default params from YAML.
//            def actionConfigFile = [utils.sourceDir(pipeline.context, 'library'), 'actions', this.name + '.yaml'].join('/')
//            if (this.script.fileExists(actionConfigFile)) {
//                def actionConfig = this.script.readYaml(file: actionConfigFile)
//                utils.debugLog(pipeline.context, actionConfig, "${this.fullName} action YAML CONFIG", [:], [], true)
//            }

            if (pipeline.configVersion() == 1) {
                for (actionName in ['__default', this.name, this.name + '_' + this.methodName]) {
                    if (pipeline.context && pipeline.context.params && pipeline.context.params.action && actionName in pipeline.context.params.action) {
                        defaultActionParams = utils.merge(defaultActionParams, pipeline.context.params.action[actionName])
                        utils.debugLog(defaultActionParams, defaultActionParams, "defaultActionParams after merge from ${actionName} action CONFIG", [debugMode: 'json'], [], this.params && this.params.debugEnabled)
                    }
                }
            }

            if (!this.params) {
                this.params = [:]
            }
            this.params = utils.merge(defaultActionParams, this.params)
            utils.debugLog(this.params, this.params, "this.params after merge defaultActionParams with this.params", [debugMode: 'json'], [], this.params && this.params.debugEnabled)

            def actionInstance
            try {
                actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${this.name}", true, false)?.newInstance(
                    action: this,
                    script: this.script,
                    utils: utils,
                )
            }
            catch (err) {
                this.script.echo err.toString()
                throw err
            }

            // Interpolate action params with pipeline.context variables.
            if (this.params.containsKey('interpolate') && (this.params.interpolate == 0 || this.params.interpolate == '0')) {
                this.script.echo "Action ${this.fullName}: Interpolation disabled by interpolate config directive."
            }
            else {
                this.params = utils.serializeAndDeserialize(this.params)

                try {
                    this.script.echo "Call hook_preprocess()"
                    actionInstance.hook_preprocess()
                }
                catch (err) {
                    // No preprocess defined.
                }

                try {
                    this.script.echo "Call ${this.methodName}_hook_preprocess()"
                    actionInstance."${this.methodName}_hook_preprocess"()
                }
                catch (err) {
                    // No preprocess defined.
                }

                utils.processActionParams(this, pipeline.context, [this.name.toUpperCase(), (this.name + '_' + this.methodName).toUpperCase()])
                // TODO: Store processed action params in pipeline.context (pipeline.context.actions['action_name']) to allow use it for interpolation in other actions.
            }

            utils.debugLog(this.params, this.params, "this.params PROCESSED", [debugMode: 'json'], [], this.params && this.params.debugEnabled)

            actionParams << this.params

            def actionFile = null

            // Process credentials.
            // TODO: Make sure only allowed credentials could be used. Control it with projects.yaml in mothership config.
            ArrayList credentials = []
            // Do not use credentials in k8s mode.
            if (actionParams.credentials && pipeline.context.containerMode != 'kubernetes') {
                actionParams.credentials.each { k, v ->
                    if (v.type == 'file') {
                        v.variable_name = v.variable_name ? v.variable_name : v.id
                        credentials << this.script.file(credentialsId: v.id, variable: v.variable_name)
                    }
                }
            }

            this.script.withCredentials(credentials) {
                def envParams = actionParams.env ? actionParams.env.collect{ k, v -> "$k=$v"} : []
                script.withEnv(envParams) {
                    // Execute action from file if exist in sources...
                    if (pipeline.context.sourcesList) {
                        for (def i = 0; i < pipeline.context.sourcesList.size(); i++) {
                            def source = pipeline.context.sourcesList[i]
                            def fileName = utils.sourcePath(pipeline.context, source.name, 'pipelines/actions/' + this.name + '.groovy')
                            utils.debugLog(actionParams, fileName, "DrupipeActionWrapper file name to check")
                            // To make sure we only check fileExists in Heavyweight executor mode.
                            if (pipeline.context.block?.nodeName && this.script.fileExists(fileName)) {
                                actionFile = this.script.load(fileName)
                                // TODO: Add timeout.
                                this.result = actionFile."${this.methodName}"(actionParams)
                            }
                        }
                    }
                    // ...Otherwise execute from class.
                    if (!actionFile) {
                        try {
//                            def actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${this.name}", true, false )?.newInstance(
//                                action: this,
//                                script: this.script,
//                                utils: utils,
//                            )

                            def action_timeout = this.params.action_timeout ? this.params.action_timeout : 120
                            this.script.timeout(action_timeout) {
                                this.result = actionInstance."${this.methodName}"()
                            }
                        }
                        catch (err) {
                            this.script.echo err.toString()
                            throw err
                        }
                    }

                    if (this.params.dump_result) {
                        utils.debugLog(pipeline.context, this.result, "action_result", [debugMode: 'json'], [], this.params.debugEnabled)
                    }

                    // Results processing.
                    if (this.params.store_action_params) {
                        contextStoreResult(this.params.store_action_params_key.tokenize('.'), pipeline.context, this.params)
                    }
                    if (this.params.store_result) {
                        if (this.params.post_process) {
                            for (result in this.params.post_process) {
                                def deepValue
                                if (result.value.type == 'result') {
                                    deepValue = utils.deepGet(this, result.value.source.tokenize('.'))
                                }
                                if (deepValue) {
                                    if (result.value.destination) {
                                        contextStoreResult(result.value.destination.tokenize('.'), this, deepValue)
                                    }
                                    if (this.params.dump_result && this.params.debugEnabled) {
                                        script.echo "SOURCE: ${result.value.source}"
                                        script.echo "DESTINATION: ${result.value.destination}"
                                        script.echo "deepValue: ${deepValue}"
                                        utils.debugLog(pipeline.context, context, "Temp context", [debugMode: 'json'], [], this.params.debugEnabled)
                                    }
                                }
                            }
                        }
                    }

                    if (pipeline.context.params && pipeline.context.params.action && pipeline.context.params.action["${name}_${methodName}"] && pipeline.context.params.action["${name}_${methodName}"].debugEnabled) {
                        utils.debugLog(pipeline.context, pipeline.context, "pipeline.context results", [debugMode: 'json'], [this.params.store_result_key], this.params.debugEnabled)
                    }
                }
            }

            if (this.params.dump_result) {
                utils.debugLog(pipeline.context, this.result, "action_result", [debugMode: 'json'], [], this.params.debugEnabled)
            }
            if (context) {
                pipeline.context = pipeline.context ? utils.merge(pipeline.context, context) : context
            }

            utils.echoDelimiter "-----> DrupipeStage: ${drupipeStageName} | DrupipeActionWrapper name: ${this.fullName} end <-"
            this.result
        }
        catch (err) {
            notification.status = 'FAILED'
            notification.message = err.getMessage()
            this.script.echo notification.message
            throw err
        }
        finally {
            if (notification.status != 'FAILED') {
                notification.status = 'SUCCESSFUL'
            }
            if (this && this.result && this.result && this.result.result) {
                notification.message = notification.message ? notification.message : ''
                notification.message = notification.message + "\n\n" + this.result.result
            }
            if (this && this.result && this.result && this.result.stdout) {
                notification.message = notification.message ? notification.message : ''
                notification.message = notification.message + "\n\n" + this.result.stdout
            }
            utils.pipelineNotify(pipeline.context, notification)
        }
    }

    def contextStoreResult(path, storeContainer, result) {
        def utils = new com.github.aroq.drupipe.Utils()
        if (!path) {
            storeContainer = storeContainer ? utils.merge(storeContainer, result) : result
            return
        }
        def pathElement = path.get(0)
        def subPath = path.subList(1, path.size())
        if (!storeContainer."${pathElement}") {
            storeContainer."${pathElement}" = [:]
        }
        if (subPath.size() > 0) {
            contextStoreResult(subPath, storeContainer."${pathElement}", result)
        } else {
            storeContainer."${pathElement}" = result
        }
    }

}
