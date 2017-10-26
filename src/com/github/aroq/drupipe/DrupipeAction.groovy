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

    def execute(c = null) {
        if (c) {
            this.context << c
        }

        this.script = this.context.pipeline.script

        def utils = new com.github.aroq.drupipe.Utils()
        def actionResult = [:]

        try {
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

            for (actionName in [this.name, this.name + '_' + this.methodName]) {
                if (context && context.params && context.params.action && actionName in context.params.action) {
                    defaultActionParams = utils.merge(defaultActionParams, context.params.action[actionName])
                }
            }
            if (!this.params) {
                this.params = [:]
            }
            this.params = utils.merge(defaultActionParams, this.params)

            // Save original (unprocessed) context.params.
            // TODO: save only needed actions.
            def contextParamsConfigFile = ['.unipipe', 'context.params.yaml'].join('/')
            if (context.params) {
                if (this.script.fileExists(contextParamsConfigFile)) {
                    this.script.sh("rm -f ${contextParamsConfigFile}")
                }
                this.script.writeYaml(file: contextParamsConfigFile, data: context.params)
            }


            if (this.methodName == 'get_replicaset_name2') {
                utils.debugLog(context, context, "Kubectl.get_replicaset_name2 BEFORE PROCESSING", [:], ['params', 'action', 'Kubectl_get_replicaset_name2'], true)
            }

            // Interpolate action params with context variables.
            if (this.params.containsKey('interpolate') && (this.params.interpolate == 0 || this.params.interpolate == '0')) {
                this.script.echo "Action ${this.fullName}: Interpolation disabled by interpolate config directive."
            }
            else {
                utils.processActionParams(this, context, [this.name.toUpperCase(), (this.name + '_' + this.methodName).toUpperCase()])
                // TODO: Store processed action params in context (context.actions['action_name']) to allow use it for interpolation in other actions.
            }

            if (this.methodName == 'get_replicaset_name2') {
                utils.debugLog(context, context, "Kubectl.get_replicaset_name2 AFTER PROCESSING", [:], ['params', 'action', 'Kubectl_get_replicaset_name2'], true)
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

                            // TODO: Move context action timeout into default action (common for all actions) params.
                            def action_timeout = this.params.action_timeout ? this.params.action_timeout : context.action_timeout
                            action_timeout = action_timeout ? action_timeout : 120
                            this.script.timeout(action_timeout) {
                                actionResult = actionInstance."${this.methodName}"()
                            }

                        }
                        catch (err) {
                            this.context.pipeline.script.echo err.toString()
                            throw err
                        }
                    }
                }
            }

            utils.echoDelimiter "-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} end <-"

            actionResult = actionResult ? actionResult : [:]

            if (this.storeResult && this.storeResult != '' && actionResult.drupipeShellResult) {
                def path = storeResult.tokenize('.')
                def stored = [:]
                contextStoreResult(path, stored, actionResult.drupipeShellResult)
                this.context.pipeline.script.echo("STORED-RESULT: ${stored}")

                actionResult << stored
            }

            // Restore original (unprocessed) context.params.
            // TODO: restore only needed actions.
            if (context.params) {
                if (this.context.pipeline.script.fileExists(contextParamsConfigFile)) {
                    this.context.params = this.context.pipeline.script.readYaml(file: contextParamsConfigFile)
                }
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
            if (actionResult.drupipeShellResult) {
                notification.message = notification.message ? notification.message : ''
                notification.message = notification.message + "\n\n" + actionResult.drupipeShellResult
            }
            utils.pipelineNotify(context, notification)
        }
    }

    def contextStoreResult(path, stored, result) {
        def pathElement = path.get(0)
        def subPath = path.subList(1, path.size())
        if (!stored.containsKey(pathElement)) {
            stored[pathElement] = [:]
        }
        if (subPath.size() > 0) {
            contextStoreResult(subPath, stored[pathElement], result)
        } else {
            stored[pathElement]= result
        }
    }

}
