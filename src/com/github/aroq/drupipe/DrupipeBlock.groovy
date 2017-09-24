package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String name

    String nodeName = 'use_default'

    Boolean withDocker = false

    String dockerImage = 'use_default'

    LinkedHashMap context = [:]

    LinkedHashMap config = [:]

    // Indicates that node is already defined for the block (e.g. in DrupipePipeline).
    Boolean blockInNode = false

    def execute(c, body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        if (c) {
            this.context = c
        }

        this.context = utils.merge(this.context, this.config)

        context.pipeline.script.echo "BLOCK NAME: ${name}"

        if (utils.isTriggeredByUser() && context.jenkinsParams[name + '_node_name']) {
            nodeName = context.jenkinsParams[name + '_node_name']
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

        if (nodeName && withDocker && context.containerMode == 'docker') {
            context.pipeline.script.echo "Execute block in ${context.containerMode} container mode"
            context.pipeline.script.echo "NODE NAME: ${nodeName}"
            context.pipeline.script.node(nodeName) {
                utils.dump(this.config, 'BLOCK-CONFIG')
                utils.dump(this.context, 'BLOCK-CONTEXT')
                context.pipeline.script.unstash('config')
                context.pipeline.script.drupipeWithDocker(context) {
                    context.pipeline.scmCheckout()
                    result = _execute(body)
                }
            }
        }
        else if (withDocker && context.containerMode == 'kubernetes') {
            context.pipeline.script.echo "Execute block in ${context.containerMode} container mode"
            if (this.blockInNode) {
                result = _execute(body)
            }
            else {
                context.pipeline.script.drupipeWithKubernetes(context) {
                    result = _execute(body)
                }
            }
        }
        else {
            context.pipeline.script.node(nodeName) {
                context.pipeline.script.echo "Execute block in non container mode"
                context.pipeline.script.sshagent([context.credentialsId]) {
                    result = _execute(body)
                }
            }
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
