package com.github.aroq.drupipe

class DrupipeAction implements Serializable {

    String action

    String name

//    String storeResult

    String methodName

    HashMap params = [:]

    HashMap notification = [:]

//    LinkedHashMap pipeline.context = [:]

    def pipeline

    def script

    def result = [context: [:], action_result: [:]]

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute() {
        def utils = new com.github.aroq.drupipe.Utils()

        this.script = pipeline.script

        try {
            // Stage name & echo.
            String drupipeStageName
            if (pipeline.context.stage) {
                drupipeStageName = "${pipeline.context.stage.name}"
            }
            else {
                drupipeStageName = 'config'
            }
            pipeline.context.drupipeStageName = drupipeStageName
            notification.name = "Action ${name}"
            notification.level = "action:${drupipeStageName}"

            utils.pipelineNotify(pipeline.context, notification << [status: 'START'])
            utils.echoDelimiter("-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} start <-")

            // Define action params.
            def actionParams = [:]
//            actionParams << ['action': this]
            def defaultActionParams = [:]

            // TODO: read action default params from YAML.
//            def actionConfigFile = [utils.sourceDir(pipeline.context, 'library'), 'actions', this.name + '.yaml'].join('/')
//            if (this.script.fileExists(actionConfigFile)) {
//                def actionConfig = this.script.readYaml(file: actionConfigFile)
//                utils.debugLog(pipeline.context, actionConfig, "${this.fullName} action YAML CONFIG", [:], [], true)
//            }

            if (this.params) {
                utils.debugLog(pipeline.context, this.params, "ACTION INIT 1 ${name}.${methodName}", [debugMode: 'json'], [], true)
            }
            utils.debugLog(pipeline.context, defaultActionParams, "defaultActionParams INIT 1 ${name}.${methodName}", [debugMode: 'json'], [], true)

            for (actionName in ['ACTION',this.name, this.name + '_' + this.methodName]) {
                if (pipeline.context && pipeline.context.params && pipeline.context.params.action && actionName in pipeline.context.params.action) {
                    script.echo "Merging params from: ${actionName}"
                    utils.debugLog(pipeline.context, pipeline.context, "pipeline.context PARAMS ACTION BEFORE MERGE", [debugMode: 'json'], ['params', 'action', 'ACTION'], true)
                    defaultActionParams = utils.merge(defaultActionParams, pipeline.context.params.action[actionName])
                    utils.debugLog(pipeline.context, pipeline.context, "pipeline.context PARAMS ACTION AFTER MERGE", [debugMode: 'json'], ['params', 'action', 'ACTION'], true)
                    utils.debugLog(pipeline.context, defaultActionParams, "defaultActionParams init 2 ${name}.${methodName}: ${actionName}", [debugMode: 'json'], [], true)
                }
            }

            if (!this.params) {
                this.params = [:]
            }
            this.params = utils.merge(defaultActionParams, this.params)

            if (this.params) {
                utils.debugLog(pipeline.context, this.params, "ACTION INIT 3 ${name}.${methodName} AFTER THIS.PARAMS MERGE", [debugMode: 'json'], [], true)
                utils.debugLog(pipeline.context, defaultActionParams, "defaultActionParams INIT 4 ${name}.${methodName} AFTER THIS.PARAMS MERGE", [debugMode: 'json'], [], true)
            }

            // Save original (unprocessed) pipeline.context.params.
            // TODO: save only needed actions.
//            def contextParamsConfigFile = ['.unipipe', 'pipeline.context.params.yaml'].join('/')
//            if (pipeline.context.params) {
//                if (this.script.fileExists(contextParamsConfigFile)) {
//                    this.script.sh("rm -f ${contextParamsConfigFile}")
//                }
//                this.script.writeYaml(file: contextParamsConfigFile, data: pipeline.context.params)
//                utils.debugLog(pipeline.context, pipeline.context, "pipeline.context PARAMS ACTION AFTER SAVE", [debugMode: 'json'], ['params', 'action', 'ACTION'], true)
//            }

            if (this.params && this.params.debugEnabled) {
                utils.debugLog(pipeline.context, this.params, "ACTION ${name}.${methodName} BEFORE PROCESSING", [debugMode: 'json'], [], true)
                utils.debugLog(pipeline.context, pipeline.context, "${name}.${methodName} BEFORE PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}_${methodName}"], true)
                utils.debugLog(pipeline.context, pipeline.context, "${name} BEFORE PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}"], true)
            }

            // Interpolate action params with pipeline.context variables.
            if (this.params.containsKey('interpolate') && (this.params.interpolate == 0 || this.params.interpolate == '0')) {
                this.script.echo "Action ${this.fullName}: Interpolation disabled by interpolate config directive."
            }
            else {
                this.params = utils.serializeAndDeserialize(this.params)
                utils.processActionParams(this, pipeline.context, [this.name.toUpperCase(), (this.name + '_' + this.methodName).toUpperCase()])
                // TODO: Store processed action params in pipeline.context (pipeline.context.actions['action_name']) to allow use it for interpolation in other actions.
            }

            if (this.params && this.params.debugEnabled) {
                utils.debugLog(pipeline.context, this.params, "ACTION ${name}.${methodName} AFTER PROCESSING", [debugMode: 'json'], [], true)
                utils.debugLog(pipeline.context, pipeline.context, "${name}.${methodName} AFTER PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}_${methodName}"], true)
//                utils.debugLog(pipeline.context, pipeline.context, "${name} AFTER PROCESSING", [debugMode: 'json'], ['params', 'action', "${name}"], true)
            }

            actionParams << this.params
//            utils.debugLog(pipeline.context, actionParams, "${this.fullName} action params")

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
                    if (pipeline.context.sourcesList) {
                        for (def i = 0; i < pipeline.context.sourcesList.size(); i++) {
                            def source = pipeline.context.sourcesList[i]
                            def fileName = utils.sourcePath(pipeline.context, source.name, 'pipelines/actions/' + this.name + '.groovy')
                            utils.debugLog(actionParams, fileName, "DrupipeAction file name to check")
                            // To make sure we only check fileExists in Heavyweight executor mode.
                            if (pipeline.context.block?.nodeName && this.script.fileExists(fileName)) {
                                actionFile = this.script.load(fileName)
                                // TODO: Add timeout.
                                this.result.action_result = actionFile."${this.methodName}"(actionParams)
                            }
                        }
                    }
                    // ...Otherwise execute from class.
                    if (!actionFile) {
                        try {
                            def actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${this.name}", true, false )?.newInstance(
                                context: pipeline.context,
                                action: this,
                                script: this.script,
                                utils: utils,
                            )

                            def action_timeout = this.params.action_timeout ? this.params.action_timeout : 120
                            this.script.timeout(action_timeout) {
                                this.result.action_result = actionInstance."${this.methodName}"()
                            }
                        }
                        catch (err) {
                            this.script.echo err.toString()
                            throw err
                        }
                    }

                    // Results processing.
                    if (this.params.store_action_params) {
                        contextStoreResult(this.params.store_action_params_key.tokenize('.'), pipeline.context, this.params)
                    }
                    if (this.params.store_result) {
                        if (this.params.post_process) {
                            for (result in this.params.post_process) {
                                if (result.value.type == 'param') {
                                    def deepValue = utils.deepGet(this.params, result.value.source.tokenize('.'))
                                    contextStoreResult(result.value.destination.tokenize('.'), this.result.action_result, deepValue)
                                }
                                if (result.value.type == 'result') {
                                    if (this.params.dump_result) {
                                        utils.debugLog(pipeline.context, this.result.action_result, "action_result", [debugMode: 'json'], [], true)
                                    }
                                    script.echo "SOURCE: ${result.value.source}"
                                    def deepValue = utils.deepGet(this.result.action_result, result.value.source.tokenize('.'))
                                    if (deepValue) {
                                        script.echo "DESTINATION: ${result.value.destination}"
                                        contextStoreResult(result.value.destination.tokenize('.'), this.result, deepValue)
                                        if (this.params.dump_result) {
                                            script.echo "deepValue: ${deepValue}"
//                                            utils.debugLog(pipeline.context, this.result.action_result, "action_result after result save", [debugMode: 'json'], [], true)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (pipeline.context.params && pipeline.context.params.action && pipeline.context.params.action["${name}_${methodName}"] && pipeline.context.params.action["${name}_${methodName}"].debugEnabled) {
                        utils.debugLog(pipeline.context, pipeline.context, "pipeline.context results", [debugMode: 'json'], [this.params.store_result_key], true)
                    }
                }
            }

            utils.echoDelimiter "-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} end <-"

            this.result.action_result = this.result.action_result ? this.result.action_result : [:]

            // Refactor it.
//            if (this.storeResult && this.storeResult != '' && this.result.action_result.stdout) {
//                def path = storeResult.tokenize('.')
//                def stored = [:]
//                contextStoreResult(path, stored, this.result.action_result.stdout)
//                pipeline.context.pipeline.script.echo("STORED-RESULT: ${stored}")
//
//                action_result << stored
//            }

            // Restore original (unprocessed) pipeline.context.params.
            // TODO: restore only needed actions.
//            if (pipeline.context.params) {
//                if (script.fileExists(contextParamsConfigFile)) {
//                    pipeline.context.params = script.readYaml(file: contextParamsConfigFile)
//                }
//                utils.debugLog(pipeline.context, pipeline.context, "pipeline.context PARAMS ACTION AFTER RESTORE", [debugMode: 'json'], ['params', 'action', 'ACTION'], true)
//            }

//            this.result.action_result = action_result
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
            if (this && this.result && this.result.action_result && this.result.action_result.result) {
                notification.message = notification.message ? notification.message : ''
                notification.message = notification.message + "\n\n" + this.result.action_result.result
            }
            if (this && this.result && this.result.action_result && this.result.action_result.stdout) {
                notification.message = notification.message ? notification.message : ''
                notification.message = notification.message + "\n\n" + this.result.action_result.stdout
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
