package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String name

    String nodeName = 'use_default'

    Boolean withDocker = false

    String dockerImage = 'use_default'

    LinkedHashMap config = [:]

    DrupipePipeline pipeline

    DrupipeStage stage

    Boolean blockInNode = false

    def utils

    def execute(body = null) {
        utils = pipeline.utils

        pipeline.context = utils.merge(pipeline.context, this.config)

        pipeline.script.echo "BLOCK NAME: ${name}"

        if (utils.isTriggeredByUser() && name instanceof CharSequence && pipeline.context.jenkinsParams[name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase() + '_node_name']) {
            nodeName = pipeline.context.jenkinsParams[name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase() + '_node_name']
        }

        if (nodeName == 'use_default') {
            nodeName = pipeline.context.nodeName
        }

        if (withDocker && dockerImage == 'use_default') {
            dockerImage = pipeline.context.dockerImage
        }
        pipeline.context.dockerImage = dockerImage

        pipeline.block = this

        if (nodeName && withDocker && context.containerMode == 'docker') {
            context.pipeline.script.echo "Execute block in ${context.containerMode} container mode"
            context.pipeline.script.echo "NODE NAME: ${nodeName}"
            context.pipeline.script.node(nodeName) {
                pipeline.context.drupipe_working_dir = [pipeline.script.pwd(), '.drupipe'].join('/')
                utils.dump(this.config, 'BLOCK-CONFIG')
                utils.dump(this.context, 'BLOCK-CONTEXT')
                // Secret option for emergency remove workspace.
                if (pipeline.context.force == '11') {
                    pipeline.script.echo 'FORCE REMOVE DIR'
                    pipeline.script.deleteDir()
                }
                pipeline.script.unstash('config')
                context.pipeline.script.drupipeWithDocker(context) {
                    // Fix for scm checkout after docman commands.
                    if (pipeline.script.fileExists(pipeline.context.projectConfigPath)) {
                        pipeline.script.dir(pipeline.context.projectConfigPath) {
                            pipeline.script.deleteDir()
                        }
                    }
                    pipeline.script.checkout pipeline.script.scm
                    _execute(body)
                }
            }
        }
        else if (withDocker && context.containerMode == 'kubernetes') {
//            context.pipeline.script.echo "Execute block in ${context.containerMode} container mode"
//            if (this.blockInNode) {
//                context.pipeline.script.echo "Pod template is already defined"
//                result = _execute(body)
//            }
//            else {
//                context.pipeline.script.echo "Pod template is not already defined"
//                context.pipeline.script.drupipeWithKubernetes(context) {
//                    result = _execute(body)
//                }
//            }
        }
        else {
            context.pipeline.script.node(nodeName) {
                context.pipeline.script.echo "Execute block in non container mode"
                pipeline.script.sshagent([pipeline.context.credentialsId]) {
                    _execute(body)
                }
            }
        }
        [:]
    }

    def _execute(body = null) {
        if (stages) {
            pipeline.executeStages(stages)
        }
        else {
            if (body) {
                body()
            }
        }
    }
}
