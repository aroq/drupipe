package com.github.aroq.drupipe

class DrupipeAction implements Serializable {

    String action

    String name

    String storeResult

    String methodName

    HashMap params = [:]

    HashMap notification = [:]

    LinkedHashMap context = [:]

    def script

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute() {
        def utils = new com.github.aroq.drupipe.Utils()
        if (context.params) {
            utils.debugLog(context, context, "CONTEXT PARAMS result results 1", [debugMode: 'json'], ['params', 'action', 'ACTION', 'results'], true)
        }
//        if (c) {
//            this.context << c
//        }

        this.script = this.context.pipeline.script

        def actionResult = [:]

        try {
            if (context.params && context.params.action && context.params.action["${name}_${methodName}"] && context.params.action["${name}_${methodName}"].debugEnabled) {
                utils.debugLog(context, this.params, "ACTION ${name}.${methodName} INIT ZERO", [debugMode: 'json'], [], true)
            }
            // Stage name & echo.
            String drupipeStageName
            if (this.context.stage) {
                drupipeStageName = "${this.context.stage.name}"
            }
            else {
                drupipeStageName = 'config'
            }

            this.context.drupipeStageName = drupipeStageName

            notification.name = "Action ${name}"
            notification.level = "action:${drupipeStageName}"

            utils.pipelineNotify(context, notification << [status: 'START'])
            utils.echoDelimiter("-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} start <-")

            // Define action params.
            def actionParams = [:]
            actionParams << ['action': this]
            def defaultActionParams = [:]

            // TODO: read action default params from YAML.
//            def actionConfigFile = [utils.sourceDir(context, 'library'), 'actions', this.name + '.yaml'].join('/')
//            if (this.script.fileExists(actionConfigFile)) {
//                def actionConfig = this.script.readYaml(file: actionConfigFile)
//                utils.debugLog(context, actionConfig, "${this.fullName} action YAML CONFIG", [:], [], true)
//            }

            if (context.params && context.params.action && context.params.action["${name}_${methodName}"] && context.params.action["${name}_${methodName}"].debugEnabled) {
                utils.debugLog(context, this.params, "ACTION ${name}.${methodName} INIT", [debugMode: 'json'], [], true)
                utils.debugLog(context, defaultActionParams, "defaultActionParams ${name}.${methodName} INIT", [debugMode: 'json'], [], true)
            }
            utils.debugLog(context, context, "CONTEXT PARAMS result results 2", [debugMode: 'json'], ['params', 'action', 'ACTION', 'results'], true)

            for (actionName in ['ACTION',this.name, this.name + '_' + this.methodName]) {
                if (context && context.params && context.params.action && actionName in context.params.action) {
                    defaultActionParams = utils.merge(defaultActionParams, context.params.action[actionName])
                    if (context.params && context.params.action && context.params.action["${name}_${methodName}"] && context.params.action["${name}_${methodName}"].debugEnabled) {
                        utils.debugLog(context, this.params, "ACTION ${name}.${methodName} INIT AFTER MERGE FROM context.params.action: ${actionName}", [debugMode: 'json'], [], true)
                        utils.debugLog(context, defaultActionParams, "defaultActionParams ${name}.${methodName} context.params.action: ${actionName}", [debugMode: 'json'], [], true)
                    }
                }
            }

            utils.debugLog(context, context, "CONTEXT PARAMS result results 3", [debugMode: 'json'], ['params', 'action', 'ACTION', 'results'], true)

            if (!this.params) {
                this.params = [:]
            }
            this.params = utils.merge(defaultActionParams, this.params)

            if (this.params && this.params.debugEnabled) {
                utils.debugLog(context, this.params, "ACTION ${name}.${methodName} INIT AFTER THIS.PARAMS MERGE", [debugMode: 'json'], [], true)
                utils.debugLog(context, defaultActionParams, "defaultActionParams ${name}.${methodName} INIT AFTER THIS.PARAMS MERGE", [debugMode: 'json'], [], true)
            }

            // Save original (unprocessed) context.params.
            // TODO: save only needed actions.
            def contextParamsConfigFile = ['.unipipe', 'context.params.yaml'].join('/')
            if (context.params) {
                if (this.script.fileExists(contextParamsConfigFile)) {
                    this.script.sh("rm -f ${contextParamsConfigFile}")
                }
                this.script.writeYaml(file: contextParamsConfigFile, data: context.params)
                utils.debugLog(context, context, "CONTEXT PARAMS result results AFTER SAVE", [debugMode: 'json'], ['params', 'action', 'ACTION', 'results'], true)
            }

            if (this.params && this.params.debugEnabled) {
                utils.debugLog(context, this.params, "ACTION ${name}.${methodName} BEFORE PROCESSING", [debugMode: 'json'], [], true)
                utils.debugLog(context, context, "${name}.${methodName} BEFORE PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}_${methodName}"], true)
                utils.debugLog(context, context, "${name} BEFORE PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}"], true)
            }

            // Interpolate action params with context variables.
            if (this.params.containsKey('interpolate') && (this.params.interpolate == 0 || this.params.interpolate == '0')) {
                this.script.echo "Action ${this.fullName}: Interpolation disabled by interpolate config directive."
            }
            else {
                utils.processActionParams(this, context, [this.name.toUpperCase(), (this.name + '_' + this.methodName).toUpperCase()])
                // TODO: Store processed action params in context (context.actions['action_name']) to allow use it for interpolation in other actions.
            }

            if (this.params && this.params.debugEnabled) {
                utils.debugLog(context, this.params, "ACTION ${name}.${methodName} AFTER PROCESSING", [debugMode: 'json'], [], true)
                utils.debugLog(context, context, "${name}.${methodName} AFTER PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}_${methodName}"], true)
                utils.debugLog(context, context, "${name} AFTER PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}"], true)
            }

            actionParams << this.params
            utils.debugLog(context, actionParams, "${this.fullName} action params")

            def actionFile = null

            // Process credentials.
            // TODO: Make sure only allowed credentials could be used. Control it with projects.yaml in mothership config.
            ArrayList credentials = []
            if (actionParams.credentials) {
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
                    if (context.sourcesList) {
                        for (def i = 0; i < context.sourcesList.size(); i++) {
                            def source = context.sourcesList[i]
                            def fileName = utils.sourcePath(context, source.name, 'pipelines/actions/' + this.name + '.groovy')
                            utils.debugLog(actionParams, fileName, "DrupipeAction file name to check")
                            // To make sure we only check fileExists in Heavyweight executor mode.
                            if (context.block?.nodeName && this.script.fileExists(fileName)) {
                                actionFile = this.script.load(fileName)
                                // TODO: Add timeout.
                                actionResult = actionFile."${this.methodName}"(actionParams)
                            }
                        }
                    }
                    // ...Otherwise execute from class.
                    if (!actionFile) {
                        try {
                            def actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${this.name}", true, false )?.newInstance(
                                context: context.clone(),
                                action: this,
                                script: this.script,
                                utils: utils,
                            )

                            def action_timeout = this.params.action_timeout ? this.params.action_timeout : 120
                            this.script.timeout(action_timeout) {
                                actionResult = actionInstance."${this.methodName}"()
                            }
                        }
                        catch (err) {
                            this.context.pipeline.script.echo err.toString()
                            throw err
                        }
                    }

                    // Results processing.
                    if (this.params.store_action_params) {
                        contextStoreResult(this.params.store_action_params_key.tokenize('.'), context, this.params)
                    }
                    if (this.params.store_result) {
//                        contextStoreResult(this.params.store_result_key.tokenize('.'), context, actionResult)
                        if (this.params.results) {
                            for (result in this.params.results) {
                                if (result.value.type == 'param') {
                                    def deepValue = utils.deepGet(this.params, result.value.source.tokenize('.'))
                                    contextStoreResult(result.value.destination.tokenize('.'), actionResult, deepValue)
                                }
                                if (result.value.type == 'result') {
                                    utils.debugLog(context, actionResult, "actionResult", [debugMode: 'json'], [], true)
                                    script.echo "SOURCE: ${result.value.source}"
                                    def deepValue = utils.deepGet(actionResult, result.value.source.tokenize('.'))
                                    script.echo "deepValue: ${deepValue}"
                                    if (deepValue) {
                                        script.echo "DESTINATION: ${result.value.destination}"
                                        contextStoreResult(result.value.destination.tokenize('.'), actionResult, deepValue)
                                        utils.debugLog(context, actionResult, "actionResult after result save", [debugMode: 'json'], [], true)
                                    }
                                }
                            }
                        }
                    }

                    if (context.params && context.params.action && context.params.action["${name}_${methodName}"] && context.params.action["${name}_${methodName}"].debugEnabled) {
                        utils.debugLog(context, context, "context results", [debugMode: 'json'], [this.params.store_result_key], true)
                    }
                }
            }

            utils.echoDelimiter "-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} end <-"

            actionResult = actionResult ? actionResult : [:]

            // Refactor it.
            if (this.storeResult && this.storeResult != '' && actionResult.stdout) {
                def path = storeResult.tokenize('.')
                def stored = [:]
                contextStoreResult(path, stored, actionResult.stdout)
                this.context.pipeline.script.echo("STORED-RESULT: ${stored}")

                actionResult << stored
            }

            // Restore original (unprocessed) context.params.
            // TODO: restore only needed actions.
            if (context.params) {
                if (this.context.pipeline.script.fileExists(contextParamsConfigFile)) {
                    this.context.params = this.context.pipeline.script.readYaml(file: contextParamsConfigFile)
                }
                utils.debugLog(context, context, "CONTEXT PARAMS result results AFTER RESTORE", [debugMode: 'json'], ['params', 'action', 'ACTION', 'results'], true)
            }

            return actionResult

        }
        catch (err) {
            notification.status = 'FAILED'
            notification.message = err.getMessage()
            this.context.pipeline.script.echo notification.message
            throw err
        }
        finally {
            if (notification.status != 'FAILED') {
                notification.status = 'SUCCESSFUL'
            }
            if (actionResult.result) {
                notification.message = notification.message ? notification.message : ''
                notification.message = notification.message + "\n\n" + actionResult.result
            }
            if (actionResult.stdout) {
                notification.message = notification.message ? notification.message : ''
                notification.message = notification.message + "\n\n" + actionResult.stdout
            }
            utils.pipelineNotify(context, notification)
        }
    }

    def contextStoreResult(path, storeContainer, result) {
        if (!path) {
            storeContainer = storeContainer ? utils.merge(storeContainer, result) : result
        }
        def pathElement = path.get(0)
        def subPath = path.subList(1, path.size())
        if (!storeContainer.containsKey(pathElement)) {
            storeContainer[pathElement] = [:]
        }
        if (subPath.size() > 0) {
            contextStoreResult(subPath, storeContainer[pathElement], result)
        } else {
            storeContainer[pathElement] = result
        }
    }

}
