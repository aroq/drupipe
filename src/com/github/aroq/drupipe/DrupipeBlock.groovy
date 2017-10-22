package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String name

    String nodeName = 'use_default'

    Boolean withDocker = false

    String dockerImage = 'use_default'

    LinkedHashMap context = [:]

    LinkedHashMap config = [:]

    def execute(c, body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        if (c) {
            this.context = c
        }

        this.context = utils.merge(this.context, this.config)

        context.pipeline.script.echo "BLOCK NAME: ${name}"

        if (utils.isTriggeredByUser() && name instanceof CharSequence && context.jenkinsParams[name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase() + '_node_name']) {
            nodeName = context.jenkinsParams[name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase() + '_node_name']
        }

        if (nodeName == 'use_default') {
            nodeName = context.nodeName
        }

        if (withDocker && dockerImage == 'use_default') {
            dockerImage = context.dockerImage
        }
        context.dockerImage = dockerImage

        def result = [:]
        context.block = this

        if (nodeName) {
            //utils.pipelineNotify(context, [name: "Block on ${nodeName}", status: 'START', level: 'block'])
            context.pipeline.script.echo "NODE NAME: ${nodeName}"
            context.pipeline.script.node(nodeName) {
                // Secret option for emergency remove workspace.
                if (context.force == '11') {
                    context.pipeline.script.echo 'FORCE REMOVE DIR'
                    context.pipeline.script.deleteDir()
                }

                context.drupipe_working_dir = [context.pipeline.script.pwd(), '.drupipe'].join('/')
                utils.dump(context, this.config, 'BLOCK-CONFIG')
                utils.dump(context, this.context, 'BLOCK-CONTEXT')
                context.pipeline.script.unstash('config')
                if (withDocker) {
                    if (context.containerMode == 'kubernetes') {
                        context.pipeline.script.drupipeWithKubernetes(context) {
//                            context.pipeline.script.checkout context.pipeline.script.scm
                            result = _execute(body)
                        }
                    }
                    else if (context.containerMode == 'docker') {
                        context.pipeline.script.drupipeWithDocker(context) {
                            // Fix for scm checkout after docman commands.
                            if (context.pipeline.script.fileExists(context.projectConfigPath)) {
                                context.pipeline.script.dir(context.projectConfigPath) {
                                    context.pipeline.script.deleteDir()
                                }
                            }
                            context.pipeline.script.checkout context.pipeline.script.scm
                            result = _execute(body)
                        }
                    }
                }
                else {
                    context.pipeline.script.sshagent([context.credentialsId]) {
                        result = _execute(body)
                    }
                }
            }
            //utils.pipelineNotify(context, [name: "Block on ${nodeName}", status: 'END', level: 'block'])
        }
        else {
            result = _execute(body)
        }

        result
    }

    def _execute(body = null) {
        if (stages) {
            context << context.pipeline.executeStages(stages, context)
        }
        else {
            if (body) {
                def result = body()
                if (result) {
                    context << body()
                }
            }
        }
        context
    }
}
