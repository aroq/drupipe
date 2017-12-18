package com.github.aroq.drupipe

import com.github.aroq.drupipe.processors.DrupipeProcessorsController

class DrupipeController implements Serializable {

    ArrayList<DrupipeBlock> blocks = []

    def context = [:]

    LinkedHashMap params = [:]

    HashMap notification = [:]

    def config = [:]

    DrupipeBlock block

    def script

    com.github.aroq.drupipe.Utils utils

    def scm

    DrupipeJob job

    DrupipeConfig drupipeConfig

    DrupipeLogger drupipeLogger

    DrupipeProcessorsController drupipeProcessorsController

    def init() {
        drupipeConfig = new DrupipeConfig(controller: this, script: script, utils: utils)
    }

    def configuration() {
        context = drupipeConfig.config(params, this)
    }

    def execute(body = null) {
        script.ansiColor('xterm') {
            context.jenkinsParams = params
            utils = new com.github.aroq.drupipe.Utils()

            notification.name = 'Build'
            notification.level = 'build'

            try {
                script.timestamps {
                    init()
                    configuration()
                    if (configVersion() > 1) {
                        script.node('master') {
                            // TODO: Bring it back.
                            // Secret option for emergency remove workspace.
                            if (context.job) {
                                def jobConfig = context.job
                                archiveObjectJsonAndYaml(jobConfig, 'job')
                                script.echo "Configuration end"
                                drupipeLogger.debugLog(context, jobConfig, 'JOB', [debugMode: 'json'], [], 'INFO')
                                job = new DrupipeJob(jobConfig)
                                job.controller = this
                            }
                        }
                        job.execute()
                    }
                    else {
                        // For version 1 configs.
                        if (body) {
                            body(this)
                        }
                        executeVersion1()
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
    }

    def executeVersion1() {
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
//                        this.utils.dump(context, trigger_job, "TRIGGER JOB ${i}")

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
        def values = actionName.tokenize(".")
        if (values.size() > 1) {
            actionMethodName = values.pop()
            actionName = actionName - ".${actionMethodName}"
        }
        else {
            actionName = 'PipelineController'
            actionMethodName = values[0]
        }

        script.echo actionName
        script.echo actionMethodName

        def actionWrapperParams = [:]
        if (actionParams) {
            actionWrapperParams = actionParams
        }

        new DrupipeActionWrapper(pipeline: this, name: actionName, methodName: actionMethodName, params: actionWrapperParams)
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
        drupipeLogger.collapsedStart "Pipeline scm checkout"
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
        drupipeLogger.collapsedEnd()
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

            def source = [
                name: 'library',
                type: 'git',
                path: '.unipipe/library',
                url: url,
                branch: ref,
                refType: type,
            ]
            drupipeConfig.drupipeSourcesController.sourceAdd(source)

            context.scripts_library_loaded = true
        }
    }

    def getParam(String param) {
        utils.deepGet(this, 'context.params.pipeline.' + param)
    }

    def executeAction(action) {
        (processPipelineAction(action)).execute()
    }

    def serializeObject(path, object, mode = 'yaml') {
        if (object) {
            if (script.fileExists(path)) {
                script.sh("rm -f ${path}")
            }
            if (mode == 'yaml') {
                script.writeYaml(file: path, data: object)
            }
            else if (mode == 'json') {
                def outJson = groovy.json.JsonOutput.toJson(object)
                script.writeFile file: path, text: outJson, encoding: 'UTF-8'
            }
        }
    }

    def archiveObject(path, object, mode = 'yaml') {
        if (object) {
            serializeObject(path, object, mode)
            this.script.archiveArtifacts artifacts: path
        }
    }

    def archiveObjectJsonAndYaml(object, String name, String prefixPath = '.unipipe/temp') {
        archiveObject("${prefixPath}/${name}.yaml", object)
        archiveObject("${prefixPath}/${name}.json", object, 'json')
    }

    def configVersion() {
        drupipeConfig.configVersion()
    }


}
