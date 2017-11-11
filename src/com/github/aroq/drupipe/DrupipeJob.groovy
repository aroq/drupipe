package com.github.aroq.drupipe

class DrupipeJob implements Serializable {

    String name

    String type

    String from

    String branch

    def triggers

    def webhooks

    def context

    DrupipePipeline pipeline

    DrupipeController controller

    def execute(body = null) {
        controller.script.echo "DrupipeJob execute - ${name}"
        pipeline.controller = controller
        pipeline.execute()
    }

}
