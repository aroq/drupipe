package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    ArrayList<DrupipeBlock> blocks = []

    LinkedHashMap context = [:]

    def script

    def execute(body = null) {
        timestamps {
//            if (!context['Config_perform']) {
//            }
            node('master') {
                configParams = drupipeAction([action: 'Config.perform'], context.clone() << params)
                context << (configParams << context)
            }

            if (context.force == '11') {
                echo 'FORCE REMOVE DIR'
                deleteDir()
            }
            if (context.checkoutSCM) {
                checkout scm
            }

            context.block = [:]
            if (context.nodeName) {
                script.node(context.nodeName) {
                    context.block.nodeName = context.nodeName
                    if (context.drupipeDocker) {
                        script.drupipeWithDocker(context) {
                            blocks.each { block ->
                                script.drupipeStages(block.stages, context)
                            }
                        }
                    }
                    else {
                        blocks.each { block ->
                            script.drupipeStages(block.stages, context)
                        }
                    }
                }
            }
            else {
                blocks.each { block ->
                    script.drupipeStages(block.stages, context)
                }
            }

            if (body) {
                def result = body(context)
                if (result) {
                    context << result
                }
            }

        }

        context
    }

    def executeStages(stagesToExecute, context) {
        def stages = processStages(stagesToExecute, context)
        stages += processStages(params.stages, context)

        for (int i = 0; i < stages.size(); i++) {
            context << stages[i].execute(context)
        }
        context
    }

    @NonCPS
    List<Stage> processStages(stages, context) {
        List<Stage> result = []
        for (item in stages) {
            result << processStage(item, context)
        }
        result
    }

    @NonCPS
    Stage processStage(stage, context) {
        if (stage instanceof Stage) {
            for (action in stage.actions) {
                def values = action.action.split("\\.")
                action.name = values[0]
                action.methodName = values[1]
            }
            stage
        }
        else {
            new Stage(name: stage.key, params: context, actions: processPipelineActionList(stage.value, context))
        }
    }

    @NonCPS
    List processPipelineActionList(actionList, context) {
        List actions = []
        for (action in actionList) {
            actions << processPipelineAction(action, context)
        }
        actions
    }

    @NonCPS
    Action processPipelineAction(action, context) {
        def actionName
        def actionParams
        if (action.getClass() == java.lang.String) {
            actionName = action
            actionParams = [:]
        }
        else {
            actionName = action.action
            actionParams = action.params
        }
        def values = actionName.split("\\.")
        new Action(name: values[0], methodName: values[1], params: actionParams, context: context)
    }

    def executePipelineActionList(actions, context) {
        def actionList = processPipelineActionList(actions, context)
        context << executeActionList(actionList, context)
    }

    def executeActionList(actionList, params) {
        try {
            for (action in actionList) {
                params << action.execute(params)
            }
            params
        }
        catch (err) {
            script.echo err.toString()
            throw err
        }

        params
    }

}
