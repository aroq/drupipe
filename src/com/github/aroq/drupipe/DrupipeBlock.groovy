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

        if (nodeName) {
            //utils.pipelineNotify(pipeline.context, [name: "Block on ${nodeName}", status: 'START', level: 'block'])
            pipeline.script.echo "NODE NAME: ${nodeName}"
            pipeline.script.node(nodeName) {
                // Secret option for emergency remove workspace.
                if (pipeline.context.force == '11') {
                    pipeline.script.echo 'FORCE REMOVE DIR'
                    pipeline.script.deleteDir()
                }

                pipeline.context.drupipe_working_dir = [pipeline.script.pwd(), '.drupipe'].join('/')
                utils.dump(pipeline.context, this.config, 'BLOCK-CONFIG')
                utils.dump(pipeline.context, pipeline.context, 'BLOCK-pipeline.context')
                pipeline.script.unstash('config')
                if (withDocker) {
                    if (pipeline.context.containerMode == 'kubernetes') {
                        pipeline.script.drupipeWithKubernetes(pipeline.context) {
//                            pipeline.script.checkout pipeline.script.scm
                            _execute(body)
                        }
                    }
                    else if (pipeline.context.containerMode == 'docker') {
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
                else {
                    pipeline.script.sshagent([pipeline.context.credentialsId]) {
                        _execute(body)
                    }
                }
            }
            //utils.pipelineNotify(pipeline.context, [name: "Block on ${nodeName}", status: 'END', level: 'block'])
        }
        else {
            _execute(body)
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
