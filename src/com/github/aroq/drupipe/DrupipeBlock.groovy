package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String name

    String nodeName

    Boolean withDocker = false

    String dockerImage = 'use_default'

    def config = [:]

    DrupipeController pipeline

    DrupipeStage stage

    Boolean blockInNode = false

    def body = null

    def utils

    def execute(b = null) {
        if (b) {
            this.body = b
        }

        utils = pipeline.utils

        // TODO: check it.
        pipeline.context = utils.merge(pipeline.context, this.config)

        pipeline.script.echo "NODE NAME BEFORE: ${nodeName}"

        // TODO: refactor it.
        if (!nodeName) {
            nodeName = getParam('nodeName')
        }
        if (withDocker && !dockerImage) {
            dockerImage = getParam('dockerImage')
        }
        pipeline.context.dockerImage = dockerImage

        // TODO: Make it work ONLY if block's selection is enabled.
//        if (utils.isTriggeredByUser() && name instanceof CharSequence && pipeline.context.jenkinsParams[name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase() + '_node_name']) {
//            nodeName = pipeline.context.jenkinsParams[name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase() + '_node_name']
//        }

        pipeline.script.echo "BLOCK NAME: ${name}"
        pipeline.script.echo "NODE NAME AFTER: ${nodeName}"

        pipeline.block = this

        if (nodeName && nodeName != 'master' && withDocker && pipeline.context.containerMode == 'docker') {
            pipeline.script.echo "Execute block in ${pipeline.context.containerMode} container mode"
            pipeline.script.echo "NODE NAME: ${nodeName}"
            pipeline.script.node(nodeName) {
                pipeline.context.drupipe_working_dir = [pipeline.script.pwd(), '.drupipe'].join('/')
//                utils.dump(this.config, 'BLOCK-CONFIG')
                // Secret option for emergency remove workspace.
                if (pipeline.context.force == '11') {
                    pipeline.script.echo 'FORCE REMOVE DIR'
                    pipeline.script.deleteDir()
                }
                pipeline.script.unstash('config')
                pipeline.script.drupipeWithDocker(pipeline.context) {
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
        else if (nodeName != 'master' && withDocker && pipeline.context.containerMode == 'kubernetes') {
            pipeline.script.echo "Execute block in ${pipeline.context.containerMode} container mode"
            if (this.blockInNode) {
                pipeline.script.echo "Pod template is already defined"
                _execute(body)
            }
            else {
                pipeline.script.echo "Pod template is not already defined"
                pipeline.script.drupipeWithKubernetes(pipeline) {
                    _execute(body)
                }
            }
        }
        else {
            pipeline.script.node(nodeName) {
                pipeline.script.echo "Execute block in non container mode"
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

    def getParam(String param) {
        utils.deepGet(this, 'context.params.block.' + param)
    }
}
