package com.github.aroq.drupipe

class DrupipeAction implements Serializable {

    String action

    String name

    String storeResult

    String methodName

    HashMap params = [:]

    HashMap notification = [:]

    LinkedHashMap context = [:]

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute(c = null) {
        if (c) {
            this.context << c
        }

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
            def tempDefaultActionParams = [:]
            for (actionName in [this.name, this.name + '_' + this.methodName]) {
                if (context && context.params && context.params.action && actionName in context.params.action) {
                    tempDefaultActionParams = utils.merge(tempDefaultActionParams, context.params.action[actionName])
                }
            }
            if (!this.params) {
                this.params = [:]
            }
            this.params = utils.merge(tempDefaultActionParams, this.params)

            // Interpolate action params with context variables.
            if (this.params.containsKey('interpolate') && (this.params.interpolate == 0 || this.params.interpolate == '0')) {
                this.context.pipeline.script.echo "Action ${this.fullName}: Interpolation disabled by interpolate config directive."
            }
            else {
                this.params = utils.processActionParams(params, context, this, [this.name.toUpperCase(), (this.name + '_' + this.methodName).toUpperCase()])
                // TODO: Store processed action params in context (context.actions['action_name']) to allow use it for interpolation in other actions.
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
                        credentials << this.context.pipeline.script.file(credentialsId: v.id, variable: v.variable_name)
                    }
                }
            }

            this.context.pipeline.script.withCredentials(credentials) {
                def envParams = actionParams.env ? actionParams.env.collect{ k, v -> "$k=$v"} : []
                this.context.pipeline.script.withEnv(envParams) {
                    // Execute action from file if exist in sources...
                    if (context.sourcesList) {
                        for (def i = 0; i < context.sourcesList.size(); i++) {
                            def source = context.sourcesList[i]
                            def fileName = utils.sourcePath(context, source.name, 'pipelines/actions/' + this.name + '.groovy')
                            utils.debugLog(actionParams, fileName, "DrupipeAction file name to check")
                            // To make sure we only check fileExists in Heavyweight executor mode.
                            if (context.block?.nodeName && this.context.pipeline.script.fileExists(fileName)) {
                                actionFile = this.context.pipeline.script.load(fileName)
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
                                script: context.pipeline.script,
                                utils: utils,
                            )

                            actionResult = actionInstance."${this.methodName}"()
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

            utils.debugLog(context, context, "AFTER ${getFullName()} Kubectl Action params: ", [:], ['params', 'action', 'Kubectl_scale_replicaset'], true)

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
        def path_element = path.get(0)
        def subpath = path.subList(1, path.size())
        if (!stored.containsKey(path_element)) {
          stored[path_element] = [:]
        }
        if (subpath.size() > 0) {
          contextStoreResult(subpath, stored[path_element], result)
        } else {
          stored[path_element]= result
        }
    }

}
