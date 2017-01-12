package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {

    ArrayList<DrupipeBlock> blocks = []

    LinkedHashMap params = [:]

    def script

    def execute() {
        script.drupipe(this.params) { context ->
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
        }
    }

    def executeStages(stagesToExecute, context) {
        stages = processStages(stagesToExecute, context)
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
                values = action.action.split("\\.")
                action.name = values[0]
                action.methodName = values[1]
            }
            stage
        }
        else {
            new Stage(name: stage.key, params: context, actions: processPipelineActionList(stage.value, context), script: this)
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
        values = actionName.split("\\.")
        new Action(name: values[0], methodName: values[1], params: actionParams, script: this, context: context)
    }

}
