package com.github.aroq.drupipe

class DrupipeController implements Serializable {

    ArrayList<DrupipeBlock> blocks = []

    ArrayList<DrupipePod> pods = []

    LinkedHashMap context = [:]

    LinkedHashMap params = [:]

    HashMap notification = [:]

    LinkedHashMap config = [:]

    DrupipeBlock block

    def script

    def utils

    def scm

    DrupipeJob job

    LinkedHashMap processFrom(LinkedHashMap object) {
        LinkedHashMap result = object.clone()
        if (object.containsKey('from')) {
            if (object.from instanceof CharSequence) {
                LinkedHashMap fromObject = utils.deepGet(this, 'context.params.' + object.from)
                if (fromObject) {
                    result = utils.merge(result, processFrom(fromObject))
                }
            }
            else {
                for (item in object.from) {
                    LinkedHashMap fromObject = utils.deepGet(this, 'context.params.' + item)
                    if (fromObject) {
                        result = utils.merge(result, processFrom(fromObject))
                    }
                }
            }
            object.remove('from')
        }
        result
    }

    def processConfigItem(object) {
        def result
        if (object instanceof Map) {
            result = [:]
            for (item in object) {
                if (item instanceof Map) {
                    result[item.key] = processConfigItem(processFrom(item.value))
                }
                else {
                    result[item.key] = item.value
                }
            }
        }
        else {
            result = object
        }
        result
    }

    def preprocessConfig() {
        if (configVersion() > 1) {
            if (context.job) {

                job = new DrupipeJob(processConfigItem(context.job) << [controller: this])
//                def tempPipeline = processFrom(context.job.pipeline)
//                if (tempPipeline) {
//                    if (tempPipeline.pods) {
//                        for (pod in pods) {
//                            tempPipeline[node.key] = processFrom(node.value)
//                        }
//                    }
//                }
//                job = new DrupipeJob(processFrom(context.job) << [controller: this, pipeline: tempPipeline])
            }
        }
    }

    int configVersion() {
        context.config_version as int
    }

    def config() {
        script.node('master') {
            script.echo "Executing pipeline"

            params.debugEnabled = params.debugEnabled && params.debugEnabled != '0' ? true : false

            utils.dump(params, params, 'PIPELINE-PARAMS')
            utils.dump(params, config, 'PIPELINE-CONFIG')

            script.drupipeAction([action: 'Config.perform', params: [jenkinsParams: params]], this)
            utils.dump(context, context, 'PIPELINE-CONTEXT')

            // Secret option for emergency remove workspace.
        }
    }

    def execute(body = null) {
        context.jenkinsParams = params
        utils = new com.github.aroq.drupipe.Utils()

        notification.name = 'Build'
        notification.level = 'build'

        try {
            script.timestamps {
                config()

                if (body) {
                    body(this)
                }

                if (configVersion() > 1) {
                    preprocessConfig()
                    script.node('master') {
                        utils.debugLog(context, job.pipeline.pods[0].name, 'JOB', [debugMode: 'json'], [], true)
                    }
                }
                else {
                    if (!blocks) {
                        script.echo 'No blocks are defined, trying to get blocks from context.jobs'
                        if (context.job) {
                            def job = context.job
                            if (job) {
                                def pipelineBlocks = context.job.pipeline && context.job.pipeline.blocks ? context.job.pipeline.blocks : []
                                if (pipelineBlocks) {
                                    for (def i = 0; i < pipelineBlocks.size(); i++) {
                                        if (context.blocks && context.blocks[pipelineBlocks[i]]) {
                                            def disable_block = []
                                            if (utils.isTriggeredByUser() && context.jenkinsParams && context.jenkinsParams.disable_block && context.jenkinsParams.disable_block instanceof CharSequence) {
                                                disable_block = context.jenkinsParams.disable_block.split(",")
                                            }
                                            if (pipelineBlocks[i] in disable_block) {
                                                script.echo "Block ${pipelineBlocks[i]} were disabled"
                                            }
                                            else {
                                                def block = context.blocks[pipelineBlocks[i]]
                                                block.name = pipelineBlocks[i]
                                                blocks << block
                                            }
                                        }
                                        else {
                                            script.echo "No pipeline block: ${pipelineBlocks[i]} is defined."
                                        }
                                    }
                                    // TODO: Remove this workaround.
                                    // Workaround to allow to dump context later.
                                    script.node('master') {
                                        context = utils.serializeAndDeserialize(context)
                                    }
                                }
                                else {
                                    // TODO: to remove after updating all configs.
                                    script.node('master') {
                                        def yamlFileName = context.job.pipeline.file ? context.job.pipeline.file : "pipelines/${context.env.JOB_BASE_NAME}.yaml"
                                        def pipelineYamlFile = "${context.projectConfigPath}/${yamlFileName}"
                                        if (script.fileExists(pipelineYamlFile)) {
                                            blocks = script.readYaml(file: pipelineYamlFile).blocks
                                        }
                                    }
                                }
                            }
                            else {
                                script.echo "No job config is defined"
                            }
                        }
                        // TODO: to remove after updating all configs.
                        else {
                            script.echo "No jobs are defined in config"
                            script.node('master') {
                                def yamlFileName = "pipelines/${context.env.JOB_BASE_NAME}.yaml"
                                def pipelineYamlFile = "${context.projectConfigPath}/${yamlFileName}"
                                if (script.fileExists(pipelineYamlFile)) {
                                    blocks = script.readYaml(file: pipelineYamlFile).blocks
                                }
                            }
                        }
                    }

                    if (blocks) {
                        if (context.containerMode == 'kubernetes') {
                            script.echo "Executing blocks in kubernetes mode"
                            script.drupipeExecuteBlocksWithKubernetes(this)
                        } else {
                            script.echo "Executing blocks in standard mode"
                            executeBlocks()
                        }
                    }
                    else {
                        script.echo "No pipeline blocks defined"
                    }

                    // Trigger other jobs if configured.
                    script.node('master') {
                        script.echo "Trigger other jobs if configured"
                        if (context.job && context.job.trigger) {
                            for (def i = 0; i < context.job.trigger.size(); i++) {
                                def trigger_job = context.job.trigger[i]

                                // Check disabled triggers.
                                def disable_trigger = []
                                if (utils.isTriggeredByUser() && context.jenkinsParams && context.jenkinsParams.disable_trigger && context.jenkinsParams.disable_trigger instanceof CharSequence) {
                                    disable_trigger = context.jenkinsParams.disable_trigger.split(",")
                                }
                                if (trigger_job.name in disable_trigger) {
                                    script.echo "Trigger job ${trigger_job.name} were disabled"
                                }
                                else {
                                    script.echo "Triggering trigger name ${trigger_job.name} and job name ${trigger_job.job}"
                                    this.utils.dump(context, trigger_job, "TRIGGER JOB ${i}")

                                    def params = []
                                    def trigger_job_name_safe = trigger_job.name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase()

                                    // Add default job params.
                                    if (context.jenkinsParams) {
                                        context.jenkinsParams.each { name, value ->
                                            params << script.string(name: name, value: String.valueOf(value))
                                        }
                                    }

                                    // Add trigger job params.
                                    if (trigger_job.params) {
                                        trigger_job.params.each { name, value ->
                                            // Check trigger job param exists in job params, use config param otherwise.
                                            def trigger_param_value = context.jenkinsParams[trigger_job_name_safe + '_' + name] ? context.jenkinsParams[trigger_job_name_safe + '_' + name] : value
                                            params << script.string(name: name, value: String.valueOf(trigger_param_value))
                                        }
                                    }

                                    script.build(job: trigger_job.job, wait: false, propagate: false, parameters: params)
                                }
                            }
                        }
                    }
                }

            }
        }
        catch (e) {
            notification.status = 'FAILED'
            throw e
        }
        finally {
            if (notification.status != 'FAILED') {
                notification.status = 'SUCCESSFUL'
            }
            utils.pipelineNotify(context, notification)
        }
    }

    def executeStages(stagesToExecute = [:]) {
        def stages = processStages(stagesToExecute)
        for (int i = 0; i < stages.size(); i++) {
            script.echo "executeStages -> stage: ${stages[i].name}"
            stages[i].execute()
        }
    }

    def executeBlocks() {
        for (def i = 0; i < blocks.size(); i++) {
            blocks[i].name = "blocks-${i}"
            blocks[i].pipeline = this
            script.echo "BLOCK EXECUTE START - ${blocks[i].name}"
            (new DrupipeBlock(blocks[i])).execute()
            script.echo "BLOCK EXECUTE END - ${blocks[i].name}"
        }
    }

    @NonCPS
    List<DrupipeStage> processStages(stages) {
        List<DrupipeStage> result = []
        for (item in stages) {
            result << processStage(item)
        }
        result
    }

    @NonCPS
    DrupipeStage processStage(s) {
        if (!(s instanceof DrupipeStage)) {
            s.pipeline = this
            s = new DrupipeStage(s)
        }
        if (s instanceof DrupipeStage) {
            for (action in s.actions) {
                def values = action.action.split("\\.")
                if (values.size() > 1) {
                    action.name = values[0]
                    action.methodName = values[1]
                }
                else {
                    action.name = 'PipelineController'
                    action.methodName = values[0]
                }
            }
            s
        }
    }

    @NonCPS
    List processPipelineActionList(actionList) {
        List actions = []
        for (action in actionList) {
            actions << processPipelineAction(action)
        }
        actions
    }

    @NonCPS
    DrupipeActionWrapper processPipelineAction(action) {
        def actionName
        def actionMethodName
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
        if (values.size() > 1) {
            actionName = values[0]
            actionMethodName = values[1]
        }
        else {
            actionName = 'PipelineController'
            actionMethodName = values[0]
        }
        if (context.params && context.params.action && context.params.action["${actionName}_${actionMethodName}"] && context.params.action["${actionName}_${actionMethodName}"].debugEnabled) {
            utils.debugLog(context, actionParams, "ACTION ${actionName}.${actionMethodName} processPipelineAction()", [debugMode: 'json'], [], true)
            utils.debugLog(context, actionParams, "ACTION ${actionName}.${actionMethodName} processPipelineAction()", [debugMode: 'json'], [], true)
        }

        script.echo actionName
        script.echo actionMethodName
        new DrupipeActionWrapper(pipeline: this, name: actionName, methodName: actionMethodName, params: actionParams)
    }

    def executePipelineActionList(actions) {
        def result = [:]
        def actionList = processPipelineActionList(actions)
        try {
            for (action in actionList) {
                def actionResult = action.execute()
                result = utils.merge(result, actionResult)
            }
            result
        }
        catch (err) {
            script.echo err.toString()
            throw err
        }
    }

    def scmCheckout(scm = null) {
        this.script.echo "Pipeline scm checkout: start"
        if (scm) {
            this.script.echo "Pipeline scm checkout: set SCM"
            this.scm = scm
        }
        else {
            this.script.echo "Pipeline scm checkout: is not set"
            if (this.scm) {
                this.script.echo "Pipeline scm checkout: use stored SCM from pipeline"
            }
            else {
                this.script.echo "Pipeline scm checkout: use job's SCM"
                this.scm = script.scm
            }
        }
        this.script.checkout this.scm
    }

    def scripts_library_load() {
        if (!context.scripts_library_loaded) {
            def url  = getParam('scripts_library.url')
            def ref  = getParam('scripts_library.ref')
            def type = getParam('scripts_library.type')

            // TODO: check for the version ref type)
            if (context.env['library.global.version']) {
                ref = context.env['library.global.version']
                type = 'tag'
                script.echo "Set drupipeLibraryBranch to ${ref} as library.global.version was set"
            }
            else {
                script.echo "ENV variable library.global.version is not set"
            }
            script.drupipeAction([
                action: 'Source.add',
                params: [
                    source: [
                        name: 'library',
                        type: 'git',
                        path: '.unipipe/library',
                        url: url,
                        branch: ref,
                        refType: type,
                    ],
                ],
            ], this)
            context.scripts_library_loaded = true
        }
    }

    def getParam(String param) {
        utils.deepGet(this, 'context.params.pipeline.' + param)
    }

}
