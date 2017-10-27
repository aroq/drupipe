package com.github.aroq.drupipe

class DrupipeAction implements Serializable {

    String action

    String name

//    String storeResult

    String methodName

    HashMap params = [:]

    HashMap notification = [:]

//    LinkedHashMap pipeline.context = [:]

    DrupipePipeline pipeline

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

            for (actionName in ['ACTION',this.name, this.name + '_' + this.methodName]) {
                if (pipeline.context && pipeline.context.params && pipeline.context.params.action && actionName in pipeline.context.params.action) {
                    defaultActionParams = utils.merge(defaultActionParams, pipeline.context.params.action[actionName])
                    utils.debugLog(defaultActionParams, defaultActionParams, "defaultActionParams after merge from ${actionName} action CONFIG", [:], [], true)
                }
            }

            if (!this.params) {
                this.params = [:]
            }
            this.params = utils.merge(defaultActionParams, this.params)
            utils.debugLog(this.params, this.params, "this.params after merge defaultActionParams with this.params", [:], [], true)

            // Interpolate action params with pipeline.context variables.
            if (this.params.containsKey('interpolate') && (this.params.interpolate == 0 || this.params.interpolate == '0')) {
                this.script.echo "Action ${this.fullName}: Interpolation disabled by interpolate config directive."
            }
            else {
                this.params = utils.serializeAndDeserialize(this.params)
                utils.processActionParams(this, pipeline.context, [this.name.toUpperCase(), (this.name + '_' + this.methodName).toUpperCase()])
                // TODO: Store processed action params in pipeline.context (pipeline.context.actions['action_name']) to allow use it for interpolation in other actions.
            }

            actionParams << this.params

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
//                                    def deepValue = utils.deepGet(this.params, result.value.source.tokenize('.'))
//                                    contextStoreResult(result.value.destination.tokenize('.'), this.result.action_result, deepValue)
                                }
                                if (result.value.type == 'result') {
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

//                    if (pipeline.context.params && pipeline.context.params.action && pipeline.context.params.action["${name}_${methodName}"] && pipeline.context.params.action["${name}_${methodName}"].debugEnabled) {
//                        utils.debugLog(pipeline.context, pipeline.context, "pipeline.context results", [debugMode: 'json'], [this.params.store_result_key], true)
//                    }
                }
            }


            this.result.action_result = this.result.action_result ? this.result.action_result : [:]
            if (this.params.dump_result) {
                utils.debugLog(pipeline.context, this.result, "action_result", [debugMode: 'json'], [], true)
            }

            // Refactor it.
//            if (this.storeResult && this.storeResult != '' && this.result.action_result.stdout) {
//                def path = storeResult.tokenize('.')
//                def stored = [:]
//                contextStoreResult(path, stored, this.result.action_result.stdout)
//                pipeline.context.pipeline.script.echo("STORED-RESULT: ${stored}")
//
//                action_result << stored
//            }

            utils.echoDelimiter "-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} end <-"
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
