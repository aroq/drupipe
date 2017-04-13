package com.github.aroq.drupipe

class DrupipeAction implements Serializable {

    String action

    String name

    String methodName

    HashMap params = [:]

    LinkedHashMap context = [:]

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute(c = null) {
        if (c) {
            this.context << c
        }

        try {
            def utils = new com.github.aroq.drupipe.Utils()

            // Stage name & echo.
            String drupipeStageName
            if (this.context.stage) {
                drupipeStageName = "${this.context.stage.name}"
            }
            else {
                drupipeStageName = 'config'
            }
            utils.echoDelimiter("-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} start <-")

            // Define action params.
            //def actionParams = this.context
            def actionParams = [:]
            actionParams << ['action': this]
            def defaultActionParams = [:]
            for (actionName in [this.name, this.name + '_' + this.methodName]) {
                if (actionName in context.defaultActionParams) {
                    defaultActionParams = utils.merge(defaultActionParams, context.defaultActionParams[actionName])
                }
            }
            if (!this.params) {
                this.params = [:]
            }
            this.params = utils.merge(defaultActionParams, this.params)
            actionParams << this.params
            utils.debugLog(context, actionParams, "${this.fullName} action params")

            // Execute action from file if exist in sources...
            def actionFile = null
            def actionResult = null
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
                        context: context,
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

            def result = [:]

            // Put action result into context.
            if (actionResult) {
                if (actionResult.returnContext) {
                    if (utils.isCollectionOrList(actionResult)) {
                        context << actionResult
                        utils.debugLog(context, actionResult, "${this.fullName} action result")
                    }
                    else {
                        // TODO: check if this should be in else clause.
                        context << ["${action.name}.${action.methodName}": actionResult]
                    }
                    context.returnContext = false
                    result = context
                }
                else {
                    result = actionResult
                }
                //context[this.fullName] = actionResult
            }

            utils.echoDelimiter "-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} end <-"

            result
        }
        catch (err) {
            this.context.pipeline.script.echo err.toString()
            throw err
        }
    }

}
